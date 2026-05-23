package com.hospital.department.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.Instant;

@Entity
@Table(name = "department")
public class Department {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "dept_name", nullable = false, unique = true)
    private String deptName;
    private String description;
    @Column(name = "head_person")
    private String headPerson;
    private String phone;
    @Column(name = "dept_types")
    private Integer deptTypes;
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Department() {}

    public Department(String deptName, String description, String headPerson, String phone, Integer deptTypes) {
        this.deptName = deptName;
        this.description = description;
        this.headPerson = headPerson;
        this.phone = phone;
        this.deptTypes = deptTypes;
    }

    public Long getId() { return id; }
    public String getDeptName() { return deptName; }
    public String getDescription() { return description; }
    public String getHeadPerson() { return headPerson; }
    public String getPhone() { return phone; }
    public Integer getDeptTypes() { return deptTypes; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setDeptName(String v) { this.deptName = v; }
    public void setDescription(String v) { this.description = v; }
    public void setHeadPerson(String v) { this.headPerson = v; }
    public void setPhone(String v) { this.phone = v; }
    public void setDeptTypes(Integer v) { this.deptTypes = v; }
}
