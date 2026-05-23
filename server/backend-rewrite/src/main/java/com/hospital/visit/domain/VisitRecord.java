package com.hospital.visit.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "visit_record")
public class VisitRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "patient_id", nullable = false)
    private Long patientId;

    @Column(name = "visit_number", nullable = false, unique = true)
    private String visitNumber;

    @Column(precision = 10, scale = 2)
    private BigDecimal fee;

    @Column(name = "keshi_types")
    private Integer keshiTypes;

    @Column(name = "visit_date", nullable = false)
    private Instant visitDate;

    @Column(name = "registration_notes", columnDefinition = "TEXT")
    private String registrationNotes;

    @Column(name = "visit_content", columnDefinition = "TEXT")
    private String visitContent;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected VisitRecord() {}

    public VisitRecord(Long patientId, String visitNumber, BigDecimal fee, Integer keshiTypes,
                       Instant visitDate, String registrationNotes, String visitContent) {
        this.patientId = patientId;
        this.visitNumber = visitNumber;
        this.fee = fee;
        this.keshiTypes = keshiTypes;
        this.visitDate = visitDate != null ? visitDate : Instant.now();
        this.registrationNotes = registrationNotes;
        this.visitContent = visitContent;
    }

    public Long getId() { return id; }
    public Long getPatientId() { return patientId; }
    public String getVisitNumber() { return visitNumber; }
    public BigDecimal getFee() { return fee; }
    public Integer getKeshiTypes() { return keshiTypes; }
    public Instant getVisitDate() { return visitDate; }
    public String getRegistrationNotes() { return registrationNotes; }
    public String getVisitContent() { return visitContent; }
    public Instant getCreatedAt() { return createdAt; }

    public void setVisitNumber(String v) { this.visitNumber = v; }
    public void setFee(BigDecimal v) { this.fee = v; }
    public void setKeshiTypes(Integer v) { this.keshiTypes = v; }
    public void setVisitDate(Instant v) { this.visitDate = v; }
    public void setRegistrationNotes(String v) { this.registrationNotes = v; }
    public void setVisitContent(String v) { this.visitContent = v; }
}
