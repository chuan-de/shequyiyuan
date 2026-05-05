package com.hospital.file.dto;

import com.hospital.file.entity.FileMetadata;
import java.time.OffsetDateTime;

public record FileMetadataResponse(
    Long id,
    String businessType,
    String businessId,
    Long uploaderId,
    String originalFilename,
    String mimeType,
    Long fileSize,
    String fileHash,
    String status,
    OffsetDateTime createdAt
) {
    public static FileMetadataResponse from(FileMetadata file) {
        return new FileMetadataResponse(file.getId(), file.getBusinessType(), file.getBusinessId(), file.getUploaderId(),
            file.getOriginalFilename(), file.getMimeType(), file.getFileSize(), file.getFileHash(), file.getStatus(), file.getCreatedAt());
    }
}
