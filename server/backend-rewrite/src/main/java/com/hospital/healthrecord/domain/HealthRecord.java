package com.hospital.healthrecord.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.Instant;

@Entity
@Table(name = "health_record")
public class HealthRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "patient_id")
    private Long patientId;

    @Column(name = "patient_name")
    private String patientName;

    @Column(name = "patient_phone")
    private String patientPhone;

    @Column(name = "patient_id_number")
    private String patientIdNumber;

    @Column(name = "patient_email")
    private String patientEmail;

    @Column(name = "title")
    private String title;

    @Column(name = "other_members", columnDefinition = "TEXT")
    private String otherMembers;

    @Column(name = "unit_types")
    private Integer unitTypes;

    @Column(name = "recorded_at")
    private Instant recordedAt;

    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private HealthRecordStatus status;

    @Column(nullable = false)
    private Long version;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected HealthRecord() {}

    public HealthRecord(Long id, Long patientId, String patientName, String patientPhone,
                        String patientIdNumber, String patientEmail,
                        String title, String otherMembers,
                        Integer unitTypes, Instant recordedAt,
                        String content, HealthRecordStatus status, Long version) {
        this.id = id;
        this.patientId = patientId;
        this.patientName = patientName;
        this.patientPhone = patientPhone;
        this.patientIdNumber = patientIdNumber;
        this.patientEmail = patientEmail;
        this.title = title;
        this.otherMembers = otherMembers;
        this.unitTypes = unitTypes;
        this.recordedAt = recordedAt;
        this.content = content;
        this.status = status;
        this.version = version != null ? version : 0L;
    }

    public Long getId() { return id; }
    public Long getPatientId() { return patientId; }
    public String getPatientName() { return patientName; }
    public String getPatientPhone() { return patientPhone; }
    public String getPatientIdNumber() { return patientIdNumber; }
    public String getPatientEmail() { return patientEmail; }
    public String getTitle() { return title; }
    public String getOtherMembers() { return otherMembers; }
    public Integer getUnitTypes() { return unitTypes; }
    public Instant getRecordedAt() { return recordedAt; }
    public String getContent() { return content; }
    public HealthRecordStatus getStatus() { return status; }
    public Long getVersion() { return version; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void setStatus(HealthRecordStatus status) { this.status = status; }
    public void setVersion(Long version) { this.version = version; }
}
