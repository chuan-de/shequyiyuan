package com.hospital.file.repository;

import com.hospital.file.entity.FileMetadata;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FileMetadataRepository extends JpaRepository<FileMetadata, Long> {
    Optional<FileMetadata> findFirstByFileHashAndStatus(String fileHash, String status);
}
