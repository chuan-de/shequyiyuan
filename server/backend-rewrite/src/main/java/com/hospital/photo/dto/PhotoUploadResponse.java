package com.hospital.photo.dto;

import java.util.UUID;

public record PhotoUploadResponse(
    UUID id,
    String url,
    String contentType,
    Long sizeBytes,
    String originalFilename
) {}
