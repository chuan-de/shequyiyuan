package com.hospital.bingli.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.Instant;

@Entity
@Table(name = "bingli_record")
public class BingliRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "yisheng_id")
    private Long yishengId;

    @Column(name = "yisheng_uuid_number")
    private String yishengUuidNumber;

    @Column(name = "yisheng_name")
    private String yishengName;

    @Column(name = "yisheng_phone")
    private String yishengPhone;

    @Column(name = "yisheng_id_number")
    private String yishengIdNumber;

    @Column(name = "yisheng_email")
    private String yishengEmail;

    @Column(name = "yonghu_id")
    private Long yonghuId;

    @Column(name = "yonghu_name")
    private String yonghuName;

    @Column(name = "yonghu_phone")
    private String yonghuPhone;

    @Column(name = "yonghu_id_number")
    private String yonghuIdNumber;

    @Column(name = "yonghu_email")
    private String yonghuEmail;

    @Column(name = "bingli_uuid_number")
    private String bingliUuidNumber;

    @Column(name = "bingli_name")
    private String bingliName;

    @Column(name = "bingli_bingqing", columnDefinition = "TEXT")
    private String bingliBingqing;

    @Column(name = "jianchaxiangmu", columnDefinition = "TEXT")
    private String jianchaxiangmu;

    @Column(name = "jianchajieguo", columnDefinition = "TEXT")
    private String jianchajieguo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BingliStatus status;

    @Column(nullable = false)
    private Long version;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected BingliRecord() {}

    public BingliRecord(Long id, Long yishengId, String yishengUuidNumber, String yishengName,
                        String yishengPhone, String yishengIdNumber, String yishengEmail,
                        Long yonghuId, String yonghuName, String yonghuPhone, String yonghuIdNumber,
                        String yonghuEmail, String bingliUuidNumber, String bingliName,
                        String bingliBingqing, String jianchaxiangmu, String jianchajieguo,
                        BingliStatus status, Long version) {
        this.id = id;
        this.yishengId = yishengId;
        this.yishengUuidNumber = yishengUuidNumber;
        this.yishengName = yishengName;
        this.yishengPhone = yishengPhone;
        this.yishengIdNumber = yishengIdNumber;
        this.yishengEmail = yishengEmail;
        this.yonghuId = yonghuId;
        this.yonghuName = yonghuName;
        this.yonghuPhone = yonghuPhone;
        this.yonghuIdNumber = yonghuIdNumber;
        this.yonghuEmail = yonghuEmail;
        this.bingliUuidNumber = bingliUuidNumber;
        this.bingliName = bingliName;
        this.bingliBingqing = bingliBingqing;
        this.jianchaxiangmu = jianchaxiangmu;
        this.jianchajieguo = jianchajieguo;
        this.status = status;
        this.version = version != null ? version : 0L;
    }

    public Long getId() { return id; }
    public Long getYishengId() { return yishengId; }
    public String getYishengUuidNumber() { return yishengUuidNumber; }
    public String getYishengName() { return yishengName; }
    public String getYishengPhone() { return yishengPhone; }
    public String getYishengIdNumber() { return yishengIdNumber; }
    public String getYishengEmail() { return yishengEmail; }
    public Long getYonghuId() { return yonghuId; }
    public String getYonghuName() { return yonghuName; }
    public String getYonghuPhone() { return yonghuPhone; }
    public String getYonghuIdNumber() { return yonghuIdNumber; }
    public String getYonghuEmail() { return yonghuEmail; }
    public String getBingliUuidNumber() { return bingliUuidNumber; }
    public String getBingliName() { return bingliName; }
    public String getBingliBingqing() { return bingliBingqing; }
    public String getJianchaxiangmu() { return jianchaxiangmu; }
    public String getJianchajieguo() { return jianchajieguo; }
    public BingliStatus getStatus() { return status; }
    public Long getVersion() { return version; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void setStatus(BingliStatus status) { this.status = status; }
    public void setVersion(Long version) { this.version = version; }
}
