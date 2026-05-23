package com.hospital.reception.service;

import com.hospital.auth.service.UserAccountService;
import com.hospital.common.NotFoundException;
import com.hospital.reception.domain.ReceptionProfile;
import com.hospital.reception.dto.ReceptionCreateRequest;
import com.hospital.reception.dto.ReceptionResponse;
import com.hospital.reception.dto.ReceptionUpdateRequest;
import com.hospital.reception.repository.ReceptionProfileRepository;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class DefaultReceptionService implements ReceptionService {

    private static final String SELECT_SQL = """
            SELECT rp.id, rp.user_id, au.username, au.enabled,
                   rp.uuid_number, rp.full_name, rp.phone, rp.email,
                   rp.sex_types, rp.created_at
            FROM reception_profile rp
            JOIN app_user au ON au.id = rp.user_id
            """;

    private final ReceptionProfileRepository profileRepo;
    private final UserAccountService userAccountService;
    private final JdbcClient jdbcClient;

    public DefaultReceptionService(ReceptionProfileRepository profileRepo,
                                    UserAccountService userAccountService,
                                    JdbcClient jdbcClient) {
        this.profileRepo = profileRepo;
        this.userAccountService = userAccountService;
        this.jdbcClient = jdbcClient;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReceptionResponse> list(String keyword) {
        String sql = SELECT_SQL + """
                WHERE (:keyword IS NULL
                    OR rp.full_name ILIKE '%' || :keyword || '%'
                    OR au.username ILIKE '%' || :keyword || '%')
                ORDER BY rp.created_at DESC
                """;
        return jdbcClient.sql(sql)
                .param("keyword", (keyword == null || keyword.isBlank()) ? null : keyword)
                .query(this::mapRow)
                .list();
    }

    @Override
    @Transactional(readOnly = true)
    public ReceptionResponse detail(Long id) {
        return jdbcClient.sql(SELECT_SQL + "WHERE rp.id = :id")
                .param("id", id)
                .query(this::mapRow)
                .optional()
                .orElseThrow(() -> new NotFoundException("Reception staff not found"));
    }

    @Override
    public ReceptionResponse create(ReceptionCreateRequest request, String actor) {
        if (request.uuidNumber() != null && !request.uuidNumber().isBlank()
                && profileRepo.existsByUuidNumber(request.uuidNumber())) {
            throw new IllegalArgumentException("Work number already registered");
        }
        if (request.phone() != null && !request.phone().isBlank()
                && profileRepo.existsByPhone(request.phone())) {
            throw new IllegalArgumentException("Phone already registered");
        }

        long userId = userAccountService.createUser(request.username(), request.password(), "RECEPTION");

        ReceptionProfile profile = new ReceptionProfile(
                userId, request.uuidNumber(), request.fullName(),
                request.sexTypes(), request.phone(), request.email());
        profile = profileRepo.save(profile);

        return new ReceptionResponse(
                profile.getId(), userId, request.username(), true,
                profile.getUuidNumber(), profile.getFullName(),
                profile.getPhone(), profile.getEmail(),
                profile.getSexTypes(), profile.getCreatedAt());
    }

    @Override
    public ReceptionResponse update(Long id, ReceptionUpdateRequest request, String actor) {
        ReceptionProfile profile = profileRepo.findById(id)
                .orElseThrow(() -> new NotFoundException("Reception staff not found"));

        if (request.uuidNumber() != null && !request.uuidNumber().isBlank()
                && profileRepo.existsByUuidNumberAndIdNot(request.uuidNumber(), id)) {
            throw new IllegalArgumentException("Work number already registered");
        }
        if (request.phone() != null && !request.phone().isBlank()
                && profileRepo.existsByPhoneAndIdNot(request.phone(), id)) {
            throw new IllegalArgumentException("Phone already registered");
        }

        profile.setUuidNumber(request.uuidNumber());
        profile.setFullName(request.fullName());
        profile.setSexTypes(request.sexTypes());
        profile.setPhone(request.phone());
        profile.setEmail(request.email());
        profileRepo.save(profile);

        return detail(id);
    }

    @Override
    public ReceptionResponse changeStatus(Long id, boolean enabled, String actor) {
        ReceptionProfile profile = profileRepo.findById(id)
                .orElseThrow(() -> new NotFoundException("Reception staff not found"));
        userAccountService.setEnabled(profile.getUserId(), enabled);
        return detail(id);
    }

    private ReceptionResponse mapRow(ResultSet rs, int rowNum) throws SQLException {
        Integer sexTypes = rs.getObject("sex_types") != null ? rs.getInt("sex_types") : null;
        return new ReceptionResponse(
                rs.getLong("id"),
                rs.getLong("user_id"),
                rs.getString("username"),
                rs.getBoolean("enabled"),
                rs.getString("uuid_number"),
                rs.getString("full_name"),
                rs.getString("phone"),
                rs.getString("email"),
                sexTypes,
                rs.getTimestamp("created_at").toInstant());
    }
}
