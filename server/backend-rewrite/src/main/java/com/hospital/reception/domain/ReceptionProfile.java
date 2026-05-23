package com.hospital.reception.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.Instant;

@Entity
@Table(name = "reception_profile")
public class ReceptionProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
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

    @Column
    private String email;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected ReceptionProfile() {}

    public ReceptionProfile(Long userId, String uuidNumber, String fullName,
                             Integer sexTypes, String phone, String email) {
        this.userId = userId;
        this.uuidNumber = uuidNumber;
        this.fullName = fullName;
        this.sexTypes = sexTypes;
        this.phone = phone;
        this.email = email;
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public String getUuidNumber() { return uuidNumber; }
    public String getFullName() { return fullName; }
    public String getPhotoUrl() { return photoUrl; }
    public Integer getSexTypes() { return sexTypes; }
    public String getPhone() { return phone; }
    public String getEmail() { return email; }
    public Instant getCreatedAt() { return createdAt; }

    public void setUuidNumber(String uuidNumber) { this.uuidNumber = uuidNumber; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public void setSexTypes(Integer sexTypes) { this.sexTypes = sexTypes; }
    public void setPhone(String phone) { this.phone = phone; }
    public void setEmail(String email) { this.email = email; }
}
