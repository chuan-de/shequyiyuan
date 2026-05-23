package com.hospital.medication.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "medication")
public class Medication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String code;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MedicationStatus status;

    @Column(nullable = false)
    private Long version;

    @Column(precision = 10, scale = 2, nullable = false)
    private BigDecimal price = BigDecimal.ZERO;

    @Column(nullable = false)
    private int stock = 0;

    @Column(name = "main_effect", columnDefinition = "TEXT")
    private String mainEffect;

    @Column(name = "side_effect", columnDefinition = "TEXT")
    private String sideEffect;

    @Column(columnDefinition = "TEXT")
    private String detail;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Medication() {}

    public Medication(Long id, String code, String name, MedicationStatus status, Long version) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.status = status;
        this.version = version != null ? version : 0L;
    }

    public Long getId() { return id; }
    public String getCode() { return code; }
    public String getName() { return name; }
    public MedicationStatus getStatus() { return status; }
    public Long getVersion() { return version; }
    public BigDecimal getPrice() { return price; }
    public int getStock() { return stock; }
    public String getMainEffect() { return mainEffect; }
    public String getSideEffect() { return sideEffect; }
    public String getDetail() { return detail; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void setCode(String code) { this.code = code; }
    public void setName(String name) { this.name = name; }
    public void setStatus(MedicationStatus status) { this.status = status; }
    public void setVersion(Long version) { this.version = version; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public void setStock(int stock) { this.stock = stock; }
    public void setMainEffect(String mainEffect) { this.mainEffect = mainEffect; }
    public void setSideEffect(String sideEffect) { this.sideEffect = sideEffect; }
    public void setDetail(String detail) { this.detail = detail; }
}
