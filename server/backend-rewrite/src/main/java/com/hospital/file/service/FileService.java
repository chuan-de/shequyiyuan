package com.hospital.file.service;

import com.hospital.file.config.FileStorageProperties;
import com.hospital.file.entity.FileMetadata;
import com.hospital.file.repository.FileMetadataRepository;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FileService {
    private final FileMetadataRepository repository;
    private final FileStorageProperties properties;

    public FileService(FileMetadataRepository repository, FileStorageProperties properties) {
        this.repository = repository;
        this.properties = properties;
    }

    public FileMetadata upload(MultipartFile file, String businessType, String businessId, Long uploaderId, boolean deduplicate) throws Exception {
        validate(file);
        String hash = sha256(file.getBytes());
        if (deduplicate) {
            Optional<FileMetadata> exists = repository.findFirstByFileHashAndStatus(hash, "ACTIVE");
            if (exists.isPresent()) return exists.get();
        }

        String safeName = UUID.randomUUID() + "-" + StringUtils.cleanPath(file.getOriginalFilename());
        Path root = Path.of(properties.getRootDir());
        Files.createDirectories(root);
        Path saved = root.resolve(safeName);
        Files.copy(file.getInputStream(), saved, StandardCopyOption.REPLACE_EXISTING);

        FileMetadata metadata = new FileMetadata();
        metadata.setBusinessType(businessType);
        metadata.setBusinessId(businessId);
        metadata.setUploaderId(uploaderId);
        metadata.setOriginalFilename(file.getOriginalFilename());
        metadata.setMimeType(file.getContentType() == null ? "application/octet-stream" : file.getContentType());
        metadata.setFileSize(file.getSize());
        metadata.setFileHash(hash);
        metadata.setStoragePath(saved.toString());
        metadata.setStatus("ACTIVE");
        return repository.save(metadata);
    }

    public FileMetadata metadata(Long id, Long userId) {
        FileMetadata file = repository.findById(id).orElseThrow(() -> new IllegalArgumentException("File not found"));
        authorize(file, userId);
        return file;
    }

    public Resource download(Long id, Long userId) {
        FileMetadata file = metadata(id, userId);
        return new FileSystemResource(file.getStoragePath());
    }

    public void delete(Long id, Long userId) throws IOException {
        FileMetadata file = metadata(id, userId);
        file.setStatus("DELETED");
        repository.save(file);
        Files.deleteIfExists(Path.of(file.getStoragePath()));
    }

    public void migrateFromDirectory(Path directory, Long uploaderId, String businessType) throws IOException {
        Files.walk(directory).filter(Files::isRegularFile).forEach(path -> {
            try {
                FileMetadata meta = new FileMetadata();
                meta.setBusinessType(businessType);
                meta.setBusinessId(null);
                meta.setUploaderId(uploaderId);
                meta.setOriginalFilename(path.getFileName().toString());
                meta.setMimeType(Files.probeContentType(path) == null ? "application/octet-stream" : Files.probeContentType(path));
                meta.setFileSize(Files.size(path));
                meta.setStoragePath(path.toString());
                meta.setFileHash(sha256(Files.readAllBytes(path)));
                meta.setStatus(Files.exists(path) ? "ACTIVE" : "INVALID");
                repository.save(meta);
            } catch (Exception ignored) {}
        });
    }

    private void validate(MultipartFile file) {
        if (file.getSize() > properties.getMaxSizeBytes()) throw new IllegalArgumentException("File too large");
        String mime = file.getContentType();
        if (mime == null || !properties.getAllowedMimeTypes().contains(mime)) throw new IllegalArgumentException("File type not allowed");
    }

    private void authorize(FileMetadata file, Long userId) {
        if (!file.getUploaderId().equals(userId)) throw new SecurityException("No permission");
    }

    private String sha256(byte[] bytes) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        return HexFormat.of().formatHex(digest.digest(bytes));
    }
}
