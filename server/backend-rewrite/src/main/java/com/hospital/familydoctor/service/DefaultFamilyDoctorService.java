package com.hospital.familydoctor.service;

import com.hospital.auth.service.UserAccountService;
import com.hospital.common.NotFoundException;
import com.hospital.familydoctor.domain.FamilyDoctorProfile;
import com.hospital.familydoctor.dto.*;
import com.hospital.familydoctor.repository.FamilyDoctorProfileRepository;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class DefaultFamilyDoctorService implements FamilyDoctorService {

    private static final String SELECT_SQL = """
        SELECT fdp.id, fdp.user_id, au.username, au.enabled,
               fdp.full_name, fdp.photo_url, fdp.sex_types,
               fdp.phone, fdp.email, fdp.created_at
        FROM family_doctor_profile fdp
        JOIN app_user au ON au.id = fdp.user_id
        """;

    private final FamilyDoctorProfileRepository profileRepo;
    private final UserAccountService userAccountService;
    private final JdbcClient jdbcClient;

    public DefaultFamilyDoctorService(FamilyDoctorProfileRepository profileRepo,
                                       UserAccountService userAccountService,
                                       JdbcClient jdbcClient) {
        this.profileRepo = profileRepo;
        this.userAccountService = userAccountService;
        this.jdbcClient = jdbcClient;
    }

    @Override
    @Transactional(readOnly = true)
    public List<FamilyDoctorResponse> list(String keyword, String fullName, Integer sexTypes) {
        String sql = SELECT_SQL + """
            WHERE (CAST(:keyword AS TEXT) IS NULL OR fdp.full_name ILIKE '%' || :keyword || '%'
                                    OR au.username ILIKE '%' || :keyword || '%')
              AND (CAST(:fullName AS TEXT) IS NULL OR fdp.full_name ILIKE '%' || :fullName || '%')
              AND (CAST(:sexTypes AS INTEGER) IS NULL OR fdp.sex_types = :sexTypes)
            ORDER BY fdp.created_at DESC
            """;
        return jdbcClient.sql(sql)
            .param("keyword", blank(keyword))
            .param("fullName", blank(fullName))
            .param("sexTypes", sexTypes)
            .query(this::mapRow).list();
    }

    @Override
    @Transactional(readOnly = true)
    public FamilyDoctorResponse detail(Long id) {
        return jdbcClient.sql(SELECT_SQL + "WHERE fdp.id = :id")
            .param("id", id)
            .query(this::mapRow).optional()
            .orElseThrow(() -> new NotFoundException("Family doctor not found"));
    }

    @Override
    public FamilyDoctorResponse create(FamilyDoctorCreateRequest req, String actor) {
        if (req.phone() != null && !req.phone().isBlank() && profileRepo.existsByPhone(req.phone()))
            throw new IllegalArgumentException("Phone already registered");

        long userId = userAccountService.createUser(req.username(), req.password(), "FAMILY_DOCTOR");

        FamilyDoctorProfile profile = new FamilyDoctorProfile(userId, req.fullName(),
            req.photoUrl(), req.sexTypes(), req.phone(), req.email());
        profile = profileRepo.save(profile);
        return detail(profile.getId());
    }

    @Override
    public FamilyDoctorResponse update(Long id, FamilyDoctorUpdateRequest req, String actor) {
        FamilyDoctorProfile profile = profileRepo.findById(id)
            .orElseThrow(() -> new NotFoundException("Family doctor not found"));
        if (req.phone() != null && !req.phone().isBlank() && profileRepo.existsByPhoneAndIdNot(req.phone(), id))
            throw new IllegalArgumentException("Phone already registered");
        profile.setFullName(req.fullName());
        profile.setPhotoUrl(req.photoUrl());
        profile.setSexTypes(req.sexTypes());
        profile.setPhone(req.phone());
        profile.setEmail(req.email());
        profileRepo.save(profile);
        return detail(id);
    }

    @Override
    public FamilyDoctorResponse changeStatus(Long id, boolean enabled, String actor) {
        FamilyDoctorProfile profile = profileRepo.findById(id)
            .orElseThrow(() -> new NotFoundException("Family doctor not found"));
        userAccountService.setEnabled(profile.getUserId(), enabled);
        return detail(id);
    }

    @Override
    public void resetPassword(Long id, String newPassword, String actor) {
        FamilyDoctorProfile profile = profileRepo.findById(id)
            .orElseThrow(() -> new NotFoundException("Family doctor not found"));
        userAccountService.resetPassword(profile.getUserId(), newPassword);
    }

    private String blank(String s) { return (s == null || s.isBlank()) ? null : s; }

    private FamilyDoctorResponse mapRow(ResultSet rs, int rowNum) throws SQLException {
        Integer sexTypes = rs.getObject("sex_types") != null ? rs.getInt("sex_types") : null;
        return new FamilyDoctorResponse(
            rs.getLong("id"), rs.getLong("user_id"), rs.getString("username"), rs.getBoolean("enabled"),
            rs.getString("full_name"), rs.getString("photo_url"),
            sexTypes, rs.getString("phone"), rs.getString("email"),
            rs.getTimestamp("created_at").toInstant());
    }
}
