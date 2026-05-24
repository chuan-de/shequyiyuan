package com.hospital.photo.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "photo")
public class Photo {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "content_type", nullable = false, length = 100)
    private String contentType;

    @Column(name = "original_filename", length = 255)
    private String originalFilename;

    @Column(name = "size_bytes", nullable = false)
    private Long sizeBytes;

    @Column(name = "data", nullable = false, columnDefinition = "BYTEA")
    private byte[] data;

    @Column(name = "uploader_id")
    private Long uploaderId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Photo() {}

    public Photo(String contentType, String originalFilename, Long sizeBytes, byte[] data, Long uploaderId) {
        this.contentType = contentType;
        this.originalFilename = originalFilename;
        this.sizeBytes = sizeBytes;
        this.data = data;
        this.uploaderId = uploaderId;
    }

    public UUID getId() { return id; }
    public String getContentType() { return contentType; }
    public String getOriginalFilename() { return originalFilename; }
    public Long getSizeBytes() { return sizeBytes; }
    public byte[] getData() { return data; }
    public Long getUploaderId() { return uploaderId; }
    public Instant getCreatedAt() { return createdAt; }
}
