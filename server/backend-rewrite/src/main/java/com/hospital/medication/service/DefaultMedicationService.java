package com.hospital.medication.service;

import com.hospital.medication.domain.Medication;
import com.hospital.medication.domain.MedicationInventoryLog;
import com.hospital.medication.domain.MedicationStatus;
import com.hospital.medication.dto.MedicationUpsertRequest;
import com.hospital.common.NotFoundException;
import com.hospital.medication.repository.MedicationInventoryLogRepository;
import com.hospital.medication.repository.MedicationRepository;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class DefaultMedicationService implements MedicationService {
    private final MedicationRepository repository;
    private final MedicationInventoryLogRepository logRepo;
    private final JdbcClient jdbcClient;

    public DefaultMedicationService(MedicationRepository repository,
                                     MedicationInventoryLogRepository logRepo,
                                     JdbcClient jdbcClient) {
        this.repository = repository;
        this.logRepo = logRepo;
        this.jdbcClient = jdbcClient;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Medication> list(String keyword, MedicationStatus status) {
        return repository.findAll().stream()
                .filter(m -> status == null || m.getStatus() == status)
                .filter(m -> keyword == null || keyword.isBlank() ||
                        m.getCode().toLowerCase().contains(keyword.toLowerCase()) ||
                        m.getName().toLowerCase().contains(keyword.toLowerCase()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Medication detail(Long id) {
        return repository.findById(id).orElseThrow(() -> new NotFoundException("Medication not found"));
    }

    @Override
    public Medication create(MedicationUpsertRequest request, String actor) {
        Medication med = new Medication(null, request.code(), request.name(), MedicationStatus.ENABLED, request.version());
        if (request.price() != null) med.setPrice(request.price());
        if (request.stock() != null) med.setStock(request.stock());
        med.setMainEffect(request.mainEffect());
        med.setSideEffect(request.sideEffect());
        med.setDetail(request.detail());
        return repository.save(med);
    }

    @Override
    public Medication update(Long id, MedicationUpsertRequest request, String actor) {
        Medication current = detail(id);
        current.setCode(request.code());
        current.setName(request.name());
        if (request.price() != null) current.setPrice(request.price());
        current.setMainEffect(request.mainEffect());
        current.setSideEffect(request.sideEffect());
        current.setDetail(request.detail());
        return repository.save(current);
    }

    @Override
    public Medication changeStatus(Long id, MedicationStatus status, String actor) {
        Medication current = detail(id);
        current.setStatus(status);
        return repository.save(current);
    }

    @Override
    @Transactional
    public void adjustInventory(Long id, int delta, String reason, String operator) {
        int updated = jdbcClient.sql("""
            UPDATE medication SET stock = stock + :delta
            WHERE id = :id AND (stock + :delta) >= 0
            """).param("delta", delta).param("id", id).update();
        if (updated == 0) {
            boolean exists = repository.existsById(id);
            if (!exists) throw new NotFoundException("Medication not found");
            throw new IllegalArgumentException("Insufficient stock");
        }
        Integer stockAfter = jdbcClient.sql("SELECT stock FROM medication WHERE id = :id")
            .param("id", id).query(Integer.class).single();
        MedicationInventoryLog log = new MedicationInventoryLog(id, delta, stockAfter, reason, operator);
        logRepo.save(log);
    }
}
