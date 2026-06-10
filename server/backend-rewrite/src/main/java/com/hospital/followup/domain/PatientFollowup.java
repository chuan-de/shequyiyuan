package com.hospital.followup.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "patient_followup")
public class PatientFollowup {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "patient_id", nullable = false)
    private Long patientId;
    @Column(name = "measured_at", nullable = false)
    private Instant measuredAt;
    private Integer systolic;
    private Integer diastolic;
    @Column(name = "blood_sugar")
    private BigDecimal bloodSugar;
    @Column(name = "height_cm")
    private BigDecimal heightCm;
    @Column(name = "weight_kg")
    private BigDecimal weightKg;
    @Column(name = "heart_rate")
    private Integer heartRate;
    @Column(columnDefinition = "TEXT")
    private String notes;
    @Column(name = "recorded_by")
    private String recordedBy;
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected PatientFollowup() {}

    public PatientFollowup(Long patientId, Instant measuredAt, Integer systolic, Integer diastolic,
                           BigDecimal bloodSugar, BigDecimal heightCm, BigDecimal weightKg,
                           Integer heartRate, String notes, String recordedBy) {
        this.patientId = patientId;
        this.measuredAt = measuredAt;
        this.systolic = systolic;
        this.diastolic = diastolic;
        this.bloodSugar = bloodSugar;
        this.heightCm = heightCm;
        this.weightKg = weightKg;
        this.heartRate = heartRate;
        this.notes = notes;
        this.recordedBy = recordedBy;
    }

    public Long getId() { return id; }
    public Long getPatientId() { return patientId; }
    public Instant getMeasuredAt() { return measuredAt; }
    public Integer getSystolic() { return systolic; }
    public Integer getDiastolic() { return diastolic; }
    public BigDecimal getBloodSugar() { return bloodSugar; }
    public BigDecimal getHeightCm() { return heightCm; }
    public BigDecimal getWeightKg() { return weightKg; }
    public Integer getHeartRate() { return heartRate; }
    public String getNotes() { return notes; }
    public String getRecordedBy() { return recordedBy; }
    public Instant getCreatedAt() { return createdAt; }

    public void setMeasuredAt(Instant v) { this.measuredAt = v; }
    public void setSystolic(Integer v) { this.systolic = v; }
    public void setDiastolic(Integer v) { this.diastolic = v; }
    public void setBloodSugar(BigDecimal v) { this.bloodSugar = v; }
    public void setHeightCm(BigDecimal v) { this.heightCm = v; }
    public void setWeightKg(BigDecimal v) { this.weightKg = v; }
    public void setHeartRate(Integer v) { this.heartRate = v; }
    public void setNotes(String v) { this.notes = v; }
}
