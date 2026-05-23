package com.hospital.medication.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.Instant;

@Entity
@Table(name = "medication_inventory_log")
public class MedicationInventoryLog {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "medication_id", nullable = false)
    private Long medicationId;

    @Column(nullable = false)
    private int delta;

    @Column(name = "stock_after", nullable = false)
    private int stockAfter;

    private String reason;

    private String operator;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected MedicationInventoryLog() {}

    public MedicationInventoryLog(Long medicationId, int delta, int stockAfter, String reason, String operator) {
        this.medicationId = medicationId;
        this.delta = delta;
        this.stockAfter = stockAfter;
        this.reason = reason;
        this.operator = operator;
    }

    public Long getId() { return id; }
    public Long getMedicationId() { return medicationId; }
    public int getDelta() { return delta; }
    public int getStockAfter() { return stockAfter; }
    public String getReason() { return reason; }
    public String getOperator() { return operator; }
    public Instant getCreatedAt() { return createdAt; }
}
