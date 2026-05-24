package com.hospital.photo.controller;

import com.hospital.common.ApiResponse;
import com.hospital.photo.domain.Photo;
import com.hospital.photo.dto.PhotoUploadResponse;
import com.hospital.photo.service.PhotoService;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.UUID;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/v1/photos")
public class PhotoController {

    private final PhotoService photoService;

    public PhotoController(PhotoService photoService) {
        this.photoService = photoService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<PhotoUploadResponse> upload(@RequestPart("file") MultipartFile file,
                                                   java.security.Principal principal,
                                                   HttpServletRequest request) throws IOException {
        String username = principal != null ? principal.getName() : "anonymous";
        Photo saved = photoService.upload(file, username);

        String url = ServletUriComponentsBuilder.fromContextPath(request)
            .path("/api/v1/photos/{id}/content")
            .buildAndExpand(saved.getId())
            .toUriString();

        return ApiResponse.ok(new PhotoUploadResponse(
            saved.getId(), url, saved.getContentType(), saved.getSizeBytes(), saved.getOriginalFilename()
        ));
    }

    @GetMapping("/{id}/content")
    public ResponseEntity<byte[]> content(@PathVariable UUID id) {
        Photo photo = photoService.get(id);
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(photo.getContentType()))
            .cacheControl(CacheControl.maxAge(java.time.Duration.ofDays(7)).cachePublic())
            .body(photo.getData());
    }
}
