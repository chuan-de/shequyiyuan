package com.hospital.familydoctor.service;

import com.hospital.common.NotFoundException;
import com.hospital.familydoctor.domain.ContractStatus;
import com.hospital.familydoctor.domain.FamilyDoctorContract;
import com.hospital.familydoctor.dto.ContractCreateRequest;
import com.hospital.familydoctor.dto.ContractResponse;
import com.hospital.familydoctor.dto.ContractUpdateRequest;
import com.hospital.familydoctor.repository.FamilyDoctorContractRepository;
import com.hospital.familydoctor.repository.FamilyDoctorProfileRepository;
import com.hospital.patient.repository.PatientProfileRepository;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class DefaultContractService implements ContractService {

    private static final String SELECT_SQL = """
            SELECT c.id, c.patient_id, pp.full_name AS patient_name, pp.phone AS patient_phone,
                   c.family_doctor_id, fd.full_name AS doctor_name, fd.phone AS doctor_phone,
                   c.service_package, c.signed_at, c.expires_at, c.status, c.notes, c.created_at
            FROM family_doctor_contract c
            JOIN patient_profile pp ON pp.id = c.patient_id
            JOIN family_doctor_profile fd ON fd.id = c.family_doctor_id
            """;

    private final FamilyDoctorContractRepository contractRepo;
    private final PatientProfileRepository patientRepo;
    private final FamilyDoctorProfileRepository doctorRepo;
    private final JdbcClient jdbcClient;

    public DefaultContractService(FamilyDoctorContractRepository contractRepo,
                                  PatientProfileRepository patientRepo,
                                  FamilyDoctorProfileRepository doctorRepo,
                                  JdbcClient jdbcClient) {
        this.contractRepo = contractRepo;
        this.patientRepo = patientRepo;
        this.doctorRepo = doctorRepo;
        this.jdbcClient = jdbcClient;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ContractResponse> list(Long patientId, Long familyDoctorId, ContractStatus status, String patientName) {
        String sql = SELECT_SQL + """
                WHERE (CAST(:patientId AS BIGINT) IS NULL OR c.patient_id = CAST(:patientId AS BIGINT))
                  AND (CAST(:familyDoctorId AS BIGINT) IS NULL OR c.family_doctor_id = CAST(:familyDoctorId AS BIGINT))
                  AND (CAST(:status AS TEXT) IS NULL OR c.status = :status)
                  AND (CAST(:patientName AS TEXT) IS NULL OR pp.full_name ILIKE '%' || :patientName || '%')
                ORDER BY c.signed_at DESC, c.id DESC
                """;
        return jdbcClient.sql(sql)
                .param("patientId", patientId)
                .param("familyDoctorId", familyDoctorId)
                .param("status", status == null ? null : status.name())
                .param("patientName", (patientName == null || patientName.isBlank()) ? null : patientName)
                .query(this::mapRow)
                .list();
    }

    @Override
    @Transactional(readOnly = true)
    public ContractResponse detail(Long id) {
        return jdbcClient.sql(SELECT_SQL + "WHERE c.id = :id")
                .param("id", id)
                .query(this::mapRow)
                .optional()
                .orElseThrow(() -> new NotFoundException("Contract not found"));
    }

    @Override
    public ContractResponse create(ContractCreateRequest r, String actor) {
        if (!patientRepo.existsById(r.patientId())) throw new NotFoundException("Patient not found: " + r.patientId());
        if (!doctorRepo.existsById(r.familyDoctorId())) throw new NotFoundException("Family doctor not found: " + r.familyDoctorId());
        if (contractRepo.existsByPatientIdAndStatus(r.patientId(), ContractStatus.ACTIVE)) {
            throw new IllegalArgumentException("该患者已有生效中的签约，请先解约");
        }
        FamilyDoctorContract saved = contractRepo.save(new FamilyDoctorContract(
                r.patientId(), r.familyDoctorId(), r.servicePackage(),
                r.signedAt() != null ? r.signedAt() : LocalDate.now(), r.expiresAt(), r.notes()));
        return detail(saved.getId());
    }

    @Override
    public ContractResponse update(Long id, ContractUpdateRequest r, String actor) {
        FamilyDoctorContract c = contractRepo.findById(id)
                .orElseThrow(() -> new NotFoundException("Contract not found"));
        c.setServicePackage(r.servicePackage());
        c.setExpiresAt(r.expiresAt());
        c.setNotes(r.notes());
        // detail() 走 JdbcClient 直读数据库，必须先 flush JPA 脏数据。
        contractRepo.saveAndFlush(c);
        return detail(id);
    }

    @Override
    public ContractResponse changeStatus(Long id, ContractStatus targetStatus, String actor) {
        FamilyDoctorContract c = contractRepo.findById(id)
                .orElseThrow(() -> new NotFoundException("Contract not found"));
        if (targetStatus == ContractStatus.ACTIVE && c.getStatus() != ContractStatus.ACTIVE
                && contractRepo.existsByPatientIdAndStatus(c.getPatientId(), ContractStatus.ACTIVE)) {
            throw new IllegalArgumentException("该患者已有生效中的签约，无法恢复此签约");
        }
        c.setStatus(targetStatus);
        contractRepo.saveAndFlush(c);
        return detail(id);
    }

    private ContractResponse mapRow(ResultSet rs, int rowNum) throws SQLException {
        java.sql.Date signed = rs.getDate("signed_at");
        java.sql.Date expires = rs.getDate("expires_at");
        return new ContractResponse(
                rs.getLong("id"),
                rs.getLong("patient_id"),
                rs.getString("patient_name"),
                rs.getString("patient_phone"),
                rs.getLong("family_doctor_id"),
                rs.getString("doctor_name"),
                rs.getString("doctor_phone"),
                rs.getString("service_package"),
                signed != null ? signed.toLocalDate() : null,
                expires != null ? expires.toLocalDate() : null,
                rs.getString("status"),
                rs.getString("notes"),
                rs.getTimestamp("created_at").toInstant());
    }
}
