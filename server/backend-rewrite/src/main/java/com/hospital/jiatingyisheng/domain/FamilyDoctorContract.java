package com.hospital.jiatingyisheng.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.Instant;

@Entity
@Table(name = "family_doctor_contract")
public class FamilyDoctorContract {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "resident_id", nullable = false)
    private Long residentId;

    @Column(name = "doctor_id", nullable = false)
    private Long doctorId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FamilyDoctorStatus status;

    @Column(nullable = false)
    private Long version;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected FamilyDoctorContract() {}

    public FamilyDoctorContract(Long id, Long residentId, Long doctorId, FamilyDoctorStatus status, Long version) {
        this.id = id;
        this.residentId = residentId;
        this.doctorId = doctorId;
        this.status = status;
        this.version = version != null ? version : 0L;
    }

    public Long getId() { return id; }
    public Long getResidentId() { return residentId; }
    public Long getDoctorId() { return doctorId; }
    public FamilyDoctorStatus getStatus() { return status; }
    public Long getVersion() { return version; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void setResidentId(Long residentId) { this.residentId = residentId; }
    public void setDoctorId(Long doctorId) { this.doctorId = doctorId; }
    public void setStatus(FamilyDoctorStatus status) { this.status = status; }
    public void setVersion(Long version) { this.version = version; }
}
