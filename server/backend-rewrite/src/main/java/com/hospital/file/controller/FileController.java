package com.hospital.file.controller;

import com.hospital.auth.repository.AppUserRepository;
import com.hospital.common.ApiResponse;
import com.hospital.file.dto.FileMetadataResponse;
import com.hospital.file.entity.FileMetadata;
import com.hospital.file.service.FileService;
import java.nio.file.Path;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/files")
public class FileController {
    private final FileService fileService;
    private final AppUserRepository appUserRepository;

    public FileController(FileService fileService, AppUserRepository appUserRepository) {
        this.fileService = fileService;
        this.appUserRepository = appUserRepository;
    }

    @PostMapping(consumes = "multipart/form-data")
    public ApiResponse<FileMetadataResponse> upload(
        @RequestPart("file") MultipartFile file,
        @RequestParam String businessType,
        @RequestParam(required = false) String businessId,
        @RequestParam(defaultValue = "false") boolean deduplicate,
        java.security.Principal principal
    ) throws Exception {
        Long userId = currentUserId(principal.getName());
        FileMetadata saved = fileService.upload(file, businessType, businessId, userId, deduplicate);
        return ApiResponse.ok(FileMetadataResponse.from(saved));
    }

    @GetMapping("/{id}")
    public ApiResponse<FileMetadataResponse> metadata(@PathVariable Long id, java.security.Principal principal) {
        return ApiResponse.ok(FileMetadataResponse.from(fileService.metadata(id, currentUserId(principal.getName()))));
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> download(@PathVariable Long id, java.security.Principal principal) {
        FileMetadata metadata = fileService.metadata(id, currentUserId(principal.getName()));
        Resource resource = fileService.download(id, currentUserId(principal.getName()));
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + metadata.getOriginalFilename() + "\"")
            .header(HttpHeaders.CONTENT_TYPE, metadata.getMimeType())
            .body(resource);
    }

    @GetMapping("/{id}/preview")
    public ResponseEntity<Resource> preview(@PathVariable Long id, java.security.Principal principal) {
        FileMetadata metadata = fileService.metadata(id, currentUserId(principal.getName()));
        Resource resource = fileService.download(id, currentUserId(principal.getName()));
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_TYPE, metadata.getMimeType()).body(resource);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id, java.security.Principal principal) throws Exception {
        fileService.delete(id, currentUserId(principal.getName()));
        return ApiResponse.ok(null, "deleted");
    }

    @PostMapping("/migration/scan")
    public ApiResponse<Void> scanMigration(@RequestParam String path, @RequestParam String businessType, java.security.Principal principal) throws Exception {
        fileService.migrateFromDirectory(Path.of(path), currentUserId(principal.getName()), businessType);
        return ApiResponse.ok(null, "migration scan complete");
    }

    private Long currentUserId(String username) {
        return appUserRepository.findByUsername(username).orElseThrow(() -> new IllegalArgumentException("User not found")).getId();
    }
}
