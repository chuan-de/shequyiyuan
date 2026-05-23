package com.hospital.patient.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.Instant;

@Entity
@Table(name = "patient_profile")
public class PatientProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(name = "photo_url")
    private String photoUrl;

    @Column(name = "sex_types")
    private Integer sexTypes;

    @Column(unique = true)
    private String phone;

    @Column(name = "id_number", unique = true)
    private String idNumber;

    @Column
    private String email;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected PatientProfile() {}

    public PatientProfile(Long userId, String fullName, Integer sexTypes, String phone, String idNumber, String email) {
        this.userId = userId;
        this.fullName = fullName;
        this.sexTypes = sexTypes;
        this.phone = phone;
        this.idNumber = idNumber;
        this.email = email;
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public String getFullName() { return fullName; }
    public String getPhotoUrl() { return photoUrl; }
    public Integer getSexTypes() { return sexTypes; }
    public String getPhone() { return phone; }
    public String getIdNumber() { return idNumber; }
    public String getEmail() { return email; }
    public Instant getCreatedAt() { return createdAt; }

    public void setFullName(String fullName) { this.fullName = fullName; }
    public void setSexTypes(Integer sexTypes) { this.sexTypes = sexTypes; }
    public void setPhone(String phone) { this.phone = phone; }
    public void setIdNumber(String idNumber) { this.idNumber = idNumber; }
    public void setEmail(String email) { this.email = email; }
}
