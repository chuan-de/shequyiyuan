package com.hospital.familydoctor.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "family_doctor_contract")
public class FamilyDoctorContract {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "patient_id", nullable = false)
    private Long patientId;
    @Column(name = "family_doctor_id", nullable = false)
    private Long familyDoctorId;
    @Column(name = "service_package")
    private String servicePackage;
    @Column(name = "signed_at", nullable = false)
    private LocalDate signedAt;
    @Column(name = "expires_at")
    private LocalDate expiresAt;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ContractStatus status;
    @Column(columnDefinition = "TEXT")
    private String notes;
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected FamilyDoctorContract() {}

    public FamilyDoctorContract(Long patientId, Long familyDoctorId, String servicePackage,
                                LocalDate signedAt, LocalDate expiresAt, String notes) {
        this.patientId = patientId;
        this.familyDoctorId = familyDoctorId;
        this.servicePackage = servicePackage;
        this.signedAt = signedAt;
        this.expiresAt = expiresAt;
        this.status = ContractStatus.ACTIVE;
        this.notes = notes;
    }

    public Long getId() { return id; }
    public Long getPatientId() { return patientId; }
    public Long getFamilyDoctorId() { return familyDoctorId; }
    public String getServicePackage() { return servicePackage; }
    public LocalDate getSignedAt() { return signedAt; }
    public LocalDate getExpiresAt() { return expiresAt; }
    public ContractStatus getStatus() { return status; }
    public String getNotes() { return notes; }
    public Instant getCreatedAt() { return createdAt; }

    public void setServicePackage(String v) { this.servicePackage = v; }
    public void setExpiresAt(LocalDate v) { this.expiresAt = v; }
    public void setStatus(ContractStatus v) { this.status = v; }
    public void setNotes(String v) { this.notes = v; }
}
