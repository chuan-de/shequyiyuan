package com.hospital.patient.service;

import com.hospital.auth.service.UserAccountService;
import com.hospital.common.NotFoundException;
import com.hospital.patient.domain.PatientProfile;
import com.hospital.patient.dto.PatientCreateRequest;
import com.hospital.patient.dto.PatientResponse;
import com.hospital.patient.dto.PatientUpdateRequest;
import com.hospital.patient.repository.PatientProfileRepository;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class DefaultPatientService implements PatientService {

    private static final String SELECT_SQL = """
            SELECT pp.id, pp.user_id, au.username, au.enabled,
                   pp.full_name, pp.phone, pp.id_number, pp.email,
                   pp.sex_types, pp.created_at
            FROM patient_profile pp
            JOIN app_user au ON au.id = pp.user_id
            """;

    private final PatientProfileRepository profileRepo;
    private final UserAccountService userAccountService;
    private final JdbcClient jdbcClient;

    public DefaultPatientService(PatientProfileRepository profileRepo,
                                  UserAccountService userAccountService,
                                  JdbcClient jdbcClient) {
        this.profileRepo = profileRepo;
        this.userAccountService = userAccountService;
        this.jdbcClient = jdbcClient;
    }

    @Override
    @Transactional(readOnly = true)
    public List<PatientResponse> list(String keyword) {
        String sql = SELECT_SQL + """
                WHERE (CAST(:keyword AS TEXT) IS NULL
                    OR pp.full_name ILIKE '%' || :keyword || '%'
                    OR au.username ILIKE '%' || :keyword || '%')
                ORDER BY pp.created_at DESC
                """;
        return jdbcClient.sql(sql)
                .param("keyword", (keyword == null || keyword.isBlank()) ? null : keyword)
                .query(this::mapRow)
                .list();
    }

    @Override
    @Transactional(readOnly = true)
    public PatientResponse detail(Long id) {
        return jdbcClient.sql(SELECT_SQL + "WHERE pp.id = :id")
                .param("id", id)
                .query(this::mapRow)
                .optional()
                .orElseThrow(() -> new NotFoundException("Patient not found"));
    }

    @Override
    public PatientResponse create(PatientCreateRequest request, String actor) {
        if (request.phone() != null && !request.phone().isBlank()
                && profileRepo.existsByPhone(request.phone())) {
            throw new IllegalArgumentException("Phone already registered");
        }
        if (request.idNumber() != null && !request.idNumber().isBlank()
                && profileRepo.existsByIdNumber(request.idNumber())) {
            throw new IllegalArgumentException("ID number already registered");
        }

        long userId = userAccountService.createUser(request.username(), request.password(), "PATIENT");

        PatientProfile profile = new PatientProfile(
                userId, request.fullName(), request.sexTypes(),
                request.phone(), request.idNumber(), request.email());
        profile = profileRepo.save(profile);

        return new PatientResponse(
                profile.getId(), userId, request.username(), true,
                profile.getFullName(), profile.getPhone(), profile.getIdNumber(),
                profile.getEmail(), profile.getSexTypes(), profile.getCreatedAt());
    }

    @Override
    public PatientResponse update(Long id, PatientUpdateRequest request, String actor) {
        PatientProfile profile = profileRepo.findById(id)
                .orElseThrow(() -> new NotFoundException("Patient not found"));

        if (request.phone() != null && !request.phone().isBlank()
                && profileRepo.existsByPhoneAndIdNot(request.phone(), id)) {
            throw new IllegalArgumentException("Phone already registered");
        }
        if (request.idNumber() != null && !request.idNumber().isBlank()
                && profileRepo.existsByIdNumberAndIdNot(request.idNumber(), id)) {
            throw new IllegalArgumentException("ID number already registered");
        }

        profile.setFullName(request.fullName());
        profile.setSexTypes(request.sexTypes());
        profile.setPhone(request.phone());
        profile.setIdNumber(request.idNumber());
        profile.setEmail(request.email());
        profileRepo.save(profile);

        return detail(id);
    }

    @Override
    public PatientResponse changeStatus(Long id, boolean enabled, String actor) {
        PatientProfile profile = profileRepo.findById(id)
                .orElseThrow(() -> new NotFoundException("Patient not found"));
        userAccountService.setEnabled(profile.getUserId(), enabled);
        return detail(id);
    }

    @Override
    public void resetPassword(Long id, String newPassword) {
        PatientProfile profile = profileRepo.findById(id)
                .orElseThrow(() -> new NotFoundException("Patient not found"));
        userAccountService.resetPassword(profile.getUserId(), newPassword);
    }

    private PatientResponse mapRow(ResultSet rs, int rowNum) throws SQLException {
        Integer sexTypes = rs.getObject("sex_types") != null ? rs.getInt("sex_types") : null;
        return new PatientResponse(
                rs.getLong("id"),
                rs.getLong("user_id"),
                rs.getString("username"),
                rs.getBoolean("enabled"),
                rs.getString("full_name"),
                rs.getString("phone"),
                rs.getString("id_number"),
                rs.getString("email"),
                sexTypes,
                rs.getTimestamp("created_at").toInstant());
    }
}
