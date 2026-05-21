package com.hospital.jiuankangdangan.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.Instant;
import java.time.LocalDateTime;

@Entity
@Table(name = "jiuankangdangan_record")
public class JiuankangdanganRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

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

    @Column(name = "jiuankangdangan_name")
    private String jiuankangdanganName;

    @Column(name = "jiuankangdangan_qita", columnDefinition = "TEXT")
    private String jiuankangdanganQita;

    @Column(name = "jiuankangdangan_types")
    private Integer jiuankangdanganTypes;

    @Column(name = "insert_time")
    private LocalDateTime insertTime;

    @Column(name = "jiuankangdangan_content", columnDefinition = "TEXT")
    private String jiuankangdanganContent;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private JiuankangdanganStatus status;

    @Column(nullable = false)
    private Long version;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected JiuankangdanganRecord() {}

    public JiuankangdanganRecord(Long id, Long yonghuId, String yonghuName, String yonghuPhone,
                                  String yonghuIdNumber, String yonghuEmail,
                                  String jiuankangdanganName, String jiuankangdanganQita,
                                  Integer jiuankangdanganTypes, LocalDateTime insertTime,
                                  String jiuankangdanganContent, JiuankangdanganStatus status, Long version) {
        this.id = id;
        this.yonghuId = yonghuId;
        this.yonghuName = yonghuName;
        this.yonghuPhone = yonghuPhone;
        this.yonghuIdNumber = yonghuIdNumber;
        this.yonghuEmail = yonghuEmail;
        this.jiuankangdanganName = jiuankangdanganName;
        this.jiuankangdanganQita = jiuankangdanganQita;
        this.jiuankangdanganTypes = jiuankangdanganTypes;
        this.insertTime = insertTime;
        this.jiuankangdanganContent = jiuankangdanganContent;
        this.status = status;
        this.version = version != null ? version : 0L;
    }

    public Long getId() { return id; }
    public Long getYonghuId() { return yonghuId; }
    public String getYonghuName() { return yonghuName; }
    public String getYonghuPhone() { return yonghuPhone; }
    public String getYonghuIdNumber() { return yonghuIdNumber; }
    public String getYonghuEmail() { return yonghuEmail; }
    public String getJiuankangdanganName() { return jiuankangdanganName; }
    public String getJiuankangdanganQita() { return jiuankangdanganQita; }
    public Integer getJiuankangdanganTypes() { return jiuankangdanganTypes; }
    public LocalDateTime getInsertTime() { return insertTime; }
    public String getJiuankangdanganContent() { return jiuankangdanganContent; }
    public JiuankangdanganStatus getStatus() { return status; }
    public Long getVersion() { return version; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void setStatus(JiuankangdanganStatus status) { this.status = status; }
    public void setVersion(Long version) { this.version = version; }
}
