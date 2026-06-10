package com.hospital.dashboard;

import com.hospital.common.ApiResponse;
import com.hospital.systemconfig.service.SystemConfigService;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 首页仪表盘聚合接口：把各业务模块的关键计数汇成一个响应。
 * 任何已认证用户可调用；每项指标按调用者权限裁剪 —— 没有对应模块
 * read 权限的用户响应里就没有那个 key，前端按 key 是否存在渲染卡片。
 */
@RestController
@RequestMapping("/api/v1/dashboard")
public class DashboardController {

    private final JdbcClient jdbcClient;
    private final SystemConfigService systemConfigService;

    public DashboardController(JdbcClient jdbcClient, SystemConfigService systemConfigService) {
        this.jdbcClient = jdbcClient;
        this.systemConfigService = systemConfigService;
    }

    @GetMapping("/summary")
    public ApiResponse<Map<String, Long>> summary(Authentication authentication) {
        Map<String, Long> m = new LinkedHashMap<>();
        if (has(authentication, "patients:read")) {
            m.put("patientCount", count("SELECT COUNT(*) FROM patient_profile"));
        }
        if (has(authentication, "visits:read")) {
            m.put("todayVisitCount", count(
                "SELECT COUNT(*) FROM visit_record WHERE visit_date::date = CURRENT_DATE"));
        }
        if (has(authentication, "medications:read")) {
            m.put("lowStockMedicationCount", jdbcClient.sql(
                    "SELECT COUNT(*) FROM medication WHERE status = 'ENABLED' AND stock <= :threshold")
                .param("threshold", lowStockThreshold())
                .query(Long.class).single());
        }
        if (has(authentication, "family-doctor-contracts:read")) {
            m.put("activeContractCount", count("""
                SELECT COUNT(*) FROM family_doctor_contract
                WHERE status = 'ACTIVE' AND (expires_at IS NULL OR expires_at >= CURRENT_DATE)
                """));
        }
        if (has(authentication, "followups:read")) {
            m.put("recentFollowupCount", count(
                "SELECT COUNT(*) FROM patient_followup WHERE measured_at >= NOW() - INTERVAL '7 days'"));
        }
        if (has(authentication, "doctors:read")) {
            m.put("doctorCount", count("SELECT COUNT(*) FROM doctor_profile"));
        }
        return ApiResponse.ok(m);
    }

    private long lowStockThreshold() {
        try {
            String v = systemConfigService.effectiveConfigs().get("medication.low-stock-threshold");
            return v == null ? 10L : Long.parseLong(v.trim());
        } catch (NumberFormatException e) {
            return 10L;
        }
    }

    private long count(String sql) {
        return jdbcClient.sql(sql).query(Long.class).single();
    }

    private static boolean has(Authentication auth, String permission) {
        if (auth == null) return false;
        for (GrantedAuthority a : auth.getAuthorities()) {
            if (permission.equals(a.getAuthority())) return true;
        }
        return false;
    }
}
