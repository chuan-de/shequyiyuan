package com.hospital.doctor.service;

import com.hospital.auth.service.UserAccountService;
import com.hospital.common.NotFoundException;
import com.hospital.doctor.domain.DoctorProfile;
import com.hospital.doctor.dto.*;
import com.hospital.doctor.repository.DoctorProfileRepository;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class DefaultDoctorService implements DoctorService {

    private static final String SELECT_SQL = """
        SELECT dp.id, dp.user_id, au.username, au.enabled,
               dp.uuid_number, dp.full_name, dp.photo_url, dp.sex_types,
               dp.phone, dp.id_number, dp.email, dp.department_id,
               dept.dept_name AS department_name, dp.created_at
        FROM doctor_profile dp
        JOIN app_user au ON au.id = dp.user_id
        LEFT JOIN department dept ON dept.id = dp.department_id
        """;

    private final DoctorProfileRepository profileRepo;
    private final UserAccountService userAccountService;
    private final JdbcClient jdbcClient;

    public DefaultDoctorService(DoctorProfileRepository profileRepo,
                                 UserAccountService userAccountService,
                                 JdbcClient jdbcClient) {
        this.profileRepo = profileRepo;
        this.userAccountService = userAccountService;
        this.jdbcClient = jdbcClient;
    }

    @Override
    @Transactional(readOnly = true)
    public List<DoctorResponse> list(String keyword, String uuidNumber, String fullName, Integer sexTypes, Long departmentId) {
        String sql = SELECT_SQL + """
            WHERE (CAST(:keyword AS TEXT) IS NULL OR dp.full_name ILIKE '%' || :keyword || '%'
                                    OR au.username ILIKE '%' || :keyword || '%')
              AND (CAST(:uuidNumber AS TEXT) IS NULL OR dp.uuid_number ILIKE '%' || :uuidNumber || '%')
              AND (CAST(:fullName AS TEXT) IS NULL OR dp.full_name ILIKE '%' || :fullName || '%')
              AND (CAST(:sexTypes AS INTEGER) IS NULL OR dp.sex_types = :sexTypes)
              AND (CAST(:departmentId AS BIGINT) IS NULL OR dp.department_id = CAST(:departmentId AS BIGINT))
            ORDER BY dp.created_at DESC
            """;
        return jdbcClient.sql(sql)
            .param("keyword", blank(keyword))
            .param("uuidNumber", blank(uuidNumber))
            .param("fullName", blank(fullName))
            .param("sexTypes", sexTypes)
            .param("departmentId", departmentId)
            .query(this::mapRow).list();
    }

    @Override
    @Transactional(readOnly = true)
    public DoctorResponse detail(Long id) {
        return jdbcClient.sql(SELECT_SQL + "WHERE dp.id = :id")
            .param("id", id)
            .query(this::mapRow).optional()
            .orElseThrow(() -> new NotFoundException("Doctor not found"));
    }

    @Override
    public DoctorResponse create(DoctorCreateRequest req, String actor) {
        if (req.phone() != null && !req.phone().isBlank() && profileRepo.existsByPhone(req.phone()))
            throw new IllegalArgumentException("Phone already registered");
        if (req.idNumber() != null && !req.idNumber().isBlank() && profileRepo.existsByIdNumber(req.idNumber()))
            throw new IllegalArgumentException("ID number already registered");
        if (req.uuidNumber() != null && !req.uuidNumber().isBlank() && profileRepo.existsByUuidNumber(req.uuidNumber()))
            throw new IllegalArgumentException("UUID number already registered");

        long userId = userAccountService.createUser(req.username(), req.password(), "DOCTOR");

        DoctorProfile profile = new DoctorProfile(userId, req.uuidNumber(), req.fullName(),
            req.photoUrl(), req.sexTypes(), req.phone(), req.idNumber(), req.email());
        profile.setDepartmentId(req.departmentId());
        profile = profileRepo.save(profile);
        return detail(profile.getId());
    }

    @Override
    public DoctorResponse update(Long id, DoctorUpdateRequest req, String actor) {
        DoctorProfile profile = profileRepo.findById(id)
            .orElseThrow(() -> new NotFoundException("Doctor not found"));
        if (req.phone() != null && !req.phone().isBlank() && profileRepo.existsByPhoneAndIdNot(req.phone(), id))
            throw new IllegalArgumentException("Phone already registered");
        if (req.idNumber() != null && !req.idNumber().isBlank() && profileRepo.existsByIdNumberAndIdNot(req.idNumber(), id))
            throw new IllegalArgumentException("ID number already registered");
        if (req.uuidNumber() != null && !req.uuidNumber().isBlank() && profileRepo.existsByUuidNumberAndIdNot(req.uuidNumber(), id))
            throw new IllegalArgumentException("UUID number already registered");
        profile.setUuidNumber(req.uuidNumber());
        profile.setFullName(req.fullName());
        profile.setPhotoUrl(req.photoUrl());
        profile.setSexTypes(req.sexTypes());
        profile.setPhone(req.phone());
        profile.setIdNumber(req.idNumber());
        profile.setEmail(req.email());
        profile.setDepartmentId(req.departmentId());
        // detail() 走 JdbcClient 直读数据库，必须先 flush JPA 脏数据。
        profileRepo.saveAndFlush(profile);
        return detail(id);
    }

    @Override
    public DoctorResponse changeStatus(Long id, boolean enabled, String actor) {
        DoctorProfile profile = profileRepo.findById(id)
            .orElseThrow(() -> new NotFoundException("Doctor not found"));
        userAccountService.setEnabled(profile.getUserId(), enabled);
        return detail(id);
    }

    @Override
    public void resetPassword(Long id, String newPassword, String actor) {
        DoctorProfile profile = profileRepo.findById(id)
            .orElseThrow(() -> new NotFoundException("Doctor not found"));
        userAccountService.resetPassword(profile.getUserId(), newPassword);
    }

    @Override
    public void delete(Long id, String actor) {
        DoctorProfile profile = profileRepo.findById(id)
            .orElseThrow(() -> new NotFoundException("Doctor not found"));
        userAccountService.deleteUser(profile.getUserId());
    }

    private String blank(String s) { return (s == null || s.isBlank()) ? null : s; }

    private DoctorResponse mapRow(ResultSet rs, int rowNum) throws SQLException {
        Integer sexTypes = rs.getObject("sex_types") != null ? rs.getInt("sex_types") : null;
        Long departmentId = rs.getObject("department_id") != null ? rs.getLong("department_id") : null;
        return new DoctorResponse(
            rs.getLong("id"), rs.getLong("user_id"), rs.getString("username"), rs.getBoolean("enabled"),
            rs.getString("uuid_number"), rs.getString("full_name"), rs.getString("photo_url"),
            sexTypes, rs.getString("phone"), rs.getString("id_number"), rs.getString("email"),
            departmentId, rs.getString("department_name"),
            rs.getTimestamp("created_at").toInstant());
    }
}
