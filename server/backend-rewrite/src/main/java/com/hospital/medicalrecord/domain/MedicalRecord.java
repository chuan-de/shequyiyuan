package com.hospital.medicalrecord.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@Entity
@Table(name = "medical_record")
public class MedicalRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "doctor_id")
    private Long doctorId;

    @Column(name = "doctor_uuid_number")
    private String doctorUuidNumber;

    @Column(name = "doctor_name")
    private String doctorName;

    @Column(name = "doctor_phone")
    private String doctorPhone;

    @Column(name = "doctor_id_number")
    private String doctorIdNumber;

    @Column(name = "doctor_email")
    private String doctorEmail;

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

    @Column(name = "case_number")
    private String caseNumber;

    @Column(name = "case_name")
    private String caseName;

    @Column(name = "condition_desc", columnDefinition = "TEXT")
    private String conditionDesc;

    @Column(name = "exam_items", columnDefinition = "TEXT")
    private String examItems;

    @Column(name = "exam_results", columnDefinition = "TEXT")
    private String examResults;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "prescription_items", columnDefinition = "jsonb")
    private List<Map<String, Object>> prescriptionItems;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "attachments", columnDefinition = "jsonb")
    private List<Map<String, Object>> attachments;

    @Column(name = "record_date")
    private Instant recordDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MedicalRecordStatus status;

    @Column(nullable = false)
    private Long version;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected MedicalRecord() {}

    public MedicalRecord(Long id, Long doctorId, String doctorUuidNumber, String doctorName,
                         String doctorPhone, String doctorIdNumber, String doctorEmail,
                         Long patientId, String patientName, String patientPhone, String patientIdNumber,
                         String patientEmail, String caseNumber, String caseName,
                         String conditionDesc, String examItems, String examResults,
                         List<Map<String, Object>> prescriptionItems,
                         List<Map<String, Object>> attachments, Instant recordDate,
                         MedicalRecordStatus status, Long version) {
        this.id = id;
        this.doctorId = doctorId;
        this.doctorUuidNumber = doctorUuidNumber;
        this.doctorName = doctorName;
        this.doctorPhone = doctorPhone;
        this.doctorIdNumber = doctorIdNumber;
        this.doctorEmail = doctorEmail;
        this.patientId = patientId;
        this.patientName = patientName;
        this.patientPhone = patientPhone;
        this.patientIdNumber = patientIdNumber;
        this.patientEmail = patientEmail;
        this.caseNumber = caseNumber;
        this.caseName = caseName;
        this.conditionDesc = conditionDesc;
        this.examItems = examItems;
        this.examResults = examResults;
        this.prescriptionItems = prescriptionItems;
        this.attachments = attachments;
        this.recordDate = recordDate;
        this.status = status;
        this.version = version != null ? version : 0L;
    }

    public Long getId() { return id; }
    public Long getDoctorId() { return doctorId; }
    public String getDoctorUuidNumber() { return doctorUuidNumber; }
    public String getDoctorName() { return doctorName; }
    public String getDoctorPhone() { return doctorPhone; }
    public String getDoctorIdNumber() { return doctorIdNumber; }
    public String getDoctorEmail() { return doctorEmail; }
    public Long getPatientId() { return patientId; }
    public String getPatientName() { return patientName; }
    public String getPatientPhone() { return patientPhone; }
    public String getPatientIdNumber() { return patientIdNumber; }
    public String getPatientEmail() { return patientEmail; }
    public String getCaseNumber() { return caseNumber; }
    public String getCaseName() { return caseName; }
    public String getConditionDesc() { return conditionDesc; }
    public String getExamItems() { return examItems; }
    public String getExamResults() { return examResults; }
    public List<Map<String, Object>> getPrescriptionItems() { return prescriptionItems; }
    public List<Map<String, Object>> getAttachments() { return attachments; }
    public Instant getRecordDate() { return recordDate; }
    public MedicalRecordStatus getStatus() { return status; }
    public Long getVersion() { return version; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void setStatus(MedicalRecordStatus status) { this.status = status; }
    public void setVersion(Long version) { this.version = version; }
}
