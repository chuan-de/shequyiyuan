package com.hospital.doctor.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.Instant;

@Entity
@Table(name = "doctor_profile")
public class DoctorProfile {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;
    @Column(name = "uuid_number", unique = true)
    private String uuidNumber;
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
    private String email;
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected DoctorProfile() {}

    public DoctorProfile(Long userId, String uuidNumber, String fullName, String photoUrl,
                          Integer sexTypes, String phone, String idNumber, String email) {
        this.userId = userId;
        this.uuidNumber = uuidNumber;
        this.fullName = fullName;
        this.photoUrl = photoUrl;
        this.sexTypes = sexTypes;
        this.phone = phone;
        this.idNumber = idNumber;
        this.email = email;
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public String getUuidNumber() { return uuidNumber; }
    public String getFullName() { return fullName; }
    public String getPhotoUrl() { return photoUrl; }
    public Integer getSexTypes() { return sexTypes; }
    public String getPhone() { return phone; }
    public String getIdNumber() { return idNumber; }
    public String getEmail() { return email; }
    public Instant getCreatedAt() { return createdAt; }
    public void setUuidNumber(String v) { this.uuidNumber = v; }
    public void setFullName(String v) { this.fullName = v; }
    public void setPhotoUrl(String v) { this.photoUrl = v; }
    public void setSexTypes(Integer v) { this.sexTypes = v; }
    public void setPhone(String v) { this.phone = v; }
    public void setIdNumber(String v) { this.idNumber = v; }
    public void setEmail(String v) { this.email = v; }
}
