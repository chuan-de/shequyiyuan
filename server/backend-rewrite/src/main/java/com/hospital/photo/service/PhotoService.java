package com.hospital.photo.service;

import com.hospital.auth.repository.AppUserRepository;
import com.hospital.common.NotFoundException;
import com.hospital.photo.domain.Photo;
import com.hospital.photo.repository.PhotoRepository;
import java.io.IOException;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@Transactional
public class PhotoService {

    private static final Set<String> ALLOWED_TYPES = Set.of(
        "image/jpeg", "image/png", "image/gif", "image/webp", "application/pdf"
    );

    private static final long MAX_SIZE_BYTES = 10L * 1024 * 1024;

    private final PhotoRepository photoRepository;
    private final AppUserRepository appUserRepository;

    public PhotoService(PhotoRepository photoRepository, AppUserRepository appUserRepository) {
        this.photoRepository = photoRepository;
        this.appUserRepository = appUserRepository;
    }

    public Photo upload(MultipartFile file, String uploaderUsername) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("文件为空");
        }
        if (file.getSize() > MAX_SIZE_BYTES) {
            throw new IllegalArgumentException("文件不能超过 10MB");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_TYPES.contains(contentType.toLowerCase())) {
            throw new IllegalArgumentException("仅支持 JPEG/PNG/GIF/WebP 图片或 PDF 文件");
        }

        Long uploaderId = appUserRepository.findByUsername(uploaderUsername)
            .map(u -> u.getId())
            .orElse(null);

        Photo photo = new Photo(contentType, file.getOriginalFilename(), file.getSize(), file.getBytes(), uploaderId);
        return photoRepository.save(photo);
    }

    @Transactional(readOnly = true)
    public Photo get(UUID id) {
        return photoRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("图片不存在"));
    }
}
