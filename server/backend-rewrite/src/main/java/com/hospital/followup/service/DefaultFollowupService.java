package com.hospital.followup.service;

import com.hospital.common.NotFoundException;
import com.hospital.followup.domain.PatientFollowup;
import com.hospital.followup.dto.FollowupResponse;
import com.hospital.followup.dto.FollowupUpsertRequest;
import com.hospital.followup.repository.PatientFollowupRepository;
import com.hospital.patient.repository.PatientProfileRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class DefaultFollowupService implements FollowupService {

    private static final String SELECT_SQL = """
            SELECT f.id, f.patient_id, pp.full_name AS patient_name, f.measured_at,
                   f.systolic, f.diastolic, f.blood_sugar, f.height_cm, f.weight_kg,
                   f.heart_rate, f.notes, f.recorded_by, f.created_at
            FROM patient_followup f
            JOIN patient_profile pp ON pp.id = f.patient_id
            """;

    private final PatientFollowupRepository followupRepo;
    private final PatientProfileRepository patientRepo;
    private final JdbcClient jdbcClient;

    public DefaultFollowupService(PatientFollowupRepository followupRepo,
                                  PatientProfileRepository patientRepo,
                                  JdbcClient jdbcClient) {
        this.followupRepo = followupRepo;
        this.patientRepo = patientRepo;
        this.jdbcClient = jdbcClient;
    }

    @Override
    @Transactional(readOnly = true)
    public List<FollowupResponse> list(Long patientId, String patientName) {
        String sql = SELECT_SQL + """
                WHERE (CAST(:patientId AS BIGINT) IS NULL OR f.patient_id = CAST(:patientId AS BIGINT))
                  AND (CAST(:patientName AS TEXT) IS NULL OR pp.full_name ILIKE '%' || :patientName || '%')
                ORDER BY f.measured_at DESC, f.id DESC
                """;
        return jdbcClient.sql(sql)
                .param("patientId", patientId)
                .param("patientName", (patientName == null || patientName.isBlank()) ? null : patientName)
                .query(this::mapRow)
                .list();
    }

    @Override
    @Transactional(readOnly = true)
    public FollowupResponse detail(Long id) {
        return jdbcClient.sql(SELECT_SQL + "WHERE f.id = :id")
                .param("id", id)
                .query(this::mapRow)
                .optional()
                .orElseThrow(() -> new NotFoundException("Followup not found"));
    }

    @Override
    public FollowupResponse create(FollowupUpsertRequest r, String actor) {
        if (!patientRepo.existsById(r.patientId())) {
            throw new NotFoundException("Patient not found: " + r.patientId());
        }
        PatientFollowup saved = followupRepo.save(new PatientFollowup(
                r.patientId(), r.measuredAt() != null ? r.measuredAt() : Instant.now(),
                r.systolic(), r.diastolic(), r.bloodSugar(), r.heightCm(), r.weightKg(),
                r.heartRate(), r.notes(), actor));
        return detail(saved.getId());
    }

    @Override
    public FollowupResponse update(Long id, FollowupUpsertRequest r, String actor) {
        PatientFollowup f = followupRepo.findById(id)
                .orElseThrow(() -> new NotFoundException("Followup not found"));
        if (r.measuredAt() != null) f.setMeasuredAt(r.measuredAt());
        f.setSystolic(r.systolic());
        f.setDiastolic(r.diastolic());
        f.setBloodSugar(r.bloodSugar());
        f.setHeightCm(r.heightCm());
        f.setWeightKg(r.weightKg());
        f.setHeartRate(r.heartRate());
        f.setNotes(r.notes());
        // detail() 走 JdbcClient 直读数据库，必须先 flush JPA 脏数据。
        followupRepo.saveAndFlush(f);
        return detail(id);
    }

    @Override
    public void delete(Long id, String actor) {
        if (!followupRepo.existsById(id)) throw new NotFoundException("Followup not found");
        followupRepo.deleteById(id);
    }

    private FollowupResponse mapRow(ResultSet rs, int rowNum) throws SQLException {
        BigDecimal height = rs.getBigDecimal("height_cm");
        BigDecimal weight = rs.getBigDecimal("weight_kg");
        return new FollowupResponse(
                rs.getLong("id"),
                rs.getLong("patient_id"),
                rs.getString("patient_name"),
                rs.getTimestamp("measured_at").toInstant(),
                (Integer) rs.getObject("systolic"),
                (Integer) rs.getObject("diastolic"),
                rs.getBigDecimal("blood_sugar"),
                height,
                weight,
                bmi(height, weight),
                (Integer) rs.getObject("heart_rate"),
                rs.getString("notes"),
                rs.getString("recorded_by"),
                rs.getTimestamp("created_at").toInstant());
    }

    /** BMI = 体重(kg) / 身高(m)²，保留 1 位小数；身高或体重缺失时为 null。 */
    private static BigDecimal bmi(BigDecimal heightCm, BigDecimal weightKg) {
        if (heightCm == null || weightKg == null || heightCm.signum() <= 0) return null;
        BigDecimal heightM = heightCm.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
        return weightKg.divide(heightM.multiply(heightM), 1, RoundingMode.HALF_UP);
    }
}
