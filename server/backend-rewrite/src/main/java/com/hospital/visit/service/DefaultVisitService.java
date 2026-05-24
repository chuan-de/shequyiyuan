package com.hospital.visit.service;

import com.hospital.common.NotFoundException;
import com.hospital.visit.domain.VisitRecord;
import com.hospital.visit.dto.VisitCreateRequest;
import com.hospital.visit.dto.VisitResponse;
import com.hospital.visit.dto.VisitUpdateRequest;
import com.hospital.visit.repository.VisitRepository;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class DefaultVisitService implements VisitService {

    private static final String SELECT_SQL = """
        SELECT vr.id, vr.visit_number, vr.fee, vr.keshi_types, vr.visit_date,
               vr.registration_notes, vr.visit_content, vr.patient_id, vr.created_at,
               pp.full_name AS patient_full_name, pp.photo_url AS patient_photo_url,
               pp.phone AS patient_phone, pp.id_number AS patient_id_number, pp.email AS patient_email
        FROM visit_record vr
        JOIN patient_profile pp ON pp.id = vr.patient_id
        """;

    private final VisitRepository visitRepo;
    private final JdbcClient jdbcClient;

    public DefaultVisitService(VisitRepository visitRepo, JdbcClient jdbcClient) {
        this.visitRepo = visitRepo;
        this.jdbcClient = jdbcClient;
    }

    @Override
    @Transactional(readOnly = true)
    public List<VisitResponse> list(String visitNumber, Integer keshiTypes, String patientName) {
        String sql = SELECT_SQL + """
            WHERE (CAST(:visitNumber AS TEXT) IS NULL OR vr.visit_number ILIKE '%' || :visitNumber || '%')
              AND (CAST(:keshiTypes AS INTEGER) IS NULL OR vr.keshi_types = :keshiTypes)
              AND (CAST(:patientName AS TEXT) IS NULL OR pp.full_name ILIKE '%' || :patientName || '%')
            ORDER BY vr.created_at DESC
            """;
        return jdbcClient.sql(sql)
            .param("visitNumber", blank(visitNumber))
            .param("keshiTypes", keshiTypes)
            .param("patientName", blank(patientName))
            .query(this::mapRow).list();
    }

    @Override
    @Transactional(readOnly = true)
    public VisitResponse detail(Long id) {
        return jdbcClient.sql(SELECT_SQL + "WHERE vr.id = :id")
            .param("id", id)
            .query(this::mapRow).optional()
            .orElseThrow(() -> new NotFoundException("Visit not found"));
    }

    @Override
    public VisitResponse create(VisitCreateRequest req, String actor) {
        String visitNumber = (req.visitNumber() == null || req.visitNumber().isBlank())
            ? generateVisitNumber()
            : req.visitNumber();
        if (visitRepo.existsByVisitNumber(visitNumber))
            throw new IllegalArgumentException("Visit number already exists");
        VisitRecord record = new VisitRecord(req.patientId(), visitNumber, req.fee(),
            req.keshiTypes(), req.visitDate(), req.registrationNotes(), req.visitContent());
        record = visitRepo.save(record);
        return detail(record.getId());
    }

    @Override
    public VisitResponse update(Long id, VisitUpdateRequest req, String actor) {
        VisitRecord record = visitRepo.findById(id)
            .orElseThrow(() -> new NotFoundException("Visit not found"));
        record.setFee(req.fee());
        record.setKeshiTypes(req.keshiTypes());
        if (req.visitDate() != null) record.setVisitDate(req.visitDate());
        record.setRegistrationNotes(req.registrationNotes());
        record.setVisitContent(req.visitContent());
        visitRepo.save(record);
        return detail(id);
    }

    private String generateVisitNumber() {
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        String stamp = now.format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        int suffix = (int) (Math.random() * 900 + 100);
        for (int attempt = 0; attempt < 5; attempt++) {
            String candidate = "JZ" + stamp + suffix;
            if (!visitRepo.existsByVisitNumber(candidate)) return candidate;
            suffix++;
        }
        throw new IllegalStateException("Failed to generate unique visit number");
    }

    @Override
    public void delete(Long id, String actor) {
        if (!visitRepo.existsById(id)) throw new NotFoundException("Visit not found");
        visitRepo.deleteById(id);
    }

    private String blank(String s) { return (s == null || s.isBlank()) ? null : s; }

    private VisitResponse mapRow(ResultSet rs, int rowNum) throws SQLException {
        BigDecimal fee = rs.getBigDecimal("fee");
        Integer keshiTypes = rs.getObject("keshi_types") != null ? rs.getInt("keshi_types") : null;
        Instant visitDate = rs.getTimestamp("visit_date") != null ? rs.getTimestamp("visit_date").toInstant() : null;
        return new VisitResponse(
            rs.getLong("id"),
            rs.getString("visit_number"),
            fee,
            keshiTypes,
            visitDate,
            rs.getString("registration_notes"),
            rs.getString("visit_content"),
            rs.getLong("patient_id"),
            rs.getString("patient_full_name"),
            rs.getString("patient_photo_url"),
            rs.getString("patient_phone"),
            rs.getString("patient_id_number"),
            rs.getString("patient_email"),
            rs.getTimestamp("created_at").toInstant());
    }
}
