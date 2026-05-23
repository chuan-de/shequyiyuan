package com.hospital.familydoctor.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.Instant;

@Entity
@Table(name = "family_doctor_profile")
public class FamilyDoctorProfile {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
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
    private String email;
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected FamilyDoctorProfile() {}

    public FamilyDoctorProfile(Long userId, String fullName, String photoUrl,
                                Integer sexTypes, String phone, String email) {
        this.userId = userId;
        this.fullName = fullName;
        this.photoUrl = photoUrl;
        this.sexTypes = sexTypes;
        this.phone = phone;
        this.email = email;
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public String getFullName() { return fullName; }
    public String getPhotoUrl() { return photoUrl; }
    public Integer getSexTypes() { return sexTypes; }
    public String getPhone() { return phone; }
    public String getEmail() { return email; }
    public Instant getCreatedAt() { return createdAt; }
    public void setFullName(String v) { this.fullName = v; }
    public void setPhotoUrl(String v) { this.photoUrl = v; }
    public void setSexTypes(Integer v) { this.sexTypes = v; }
    public void setPhone(String v) { this.phone = v; }
    public void setEmail(String v) { this.email = v; }
}
