package com.gonerp.imagemanager.service;

import com.gonerp.imagemanager.dto.ImageInfoResponse;
import com.gonerp.imagemanager.model.ImageInfo;
import com.gonerp.imagemanager.repository.ImageInfoRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class ImageInfoService {

    private final ImageInfoRepository imageInfoRepository;

    @Value("${app.upload.dir}")
    private String uploadDir;

    public Page<ImageInfoResponse> findAll(String search, Pageable pageable) {
        return imageInfoRepository.findAllWithSearch(search, pageable)
                .map(ImageInfoResponse::from);
    }

    public ImageInfoResponse findById(Long id) {
        ImageInfo image = imageInfoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Image not found with id: " + id));
        return ImageInfoResponse.from(image);
    }

    public ImageInfoResponse create(String name, MultipartFile file) {
        String filename = storeFile(file);
        String resolvedName = (name != null && !name.isBlank()) ? name : file.getOriginalFilename();
        ImageInfo image = ImageInfo.builder()
                .name(resolvedName)
                .url("/api/images/files/" + filename)
                .build();
        return ImageInfoResponse.from(imageInfoRepository.save(image));
    }

    public ImageInfoResponse update(Long id, String name, MultipartFile file) {
        ImageInfo image = imageInfoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Image not found with id: " + id));
        image.setName(name);
        if (file != null && !file.isEmpty()) {
            deletePhysicalFile(image.getUrl());
            String filename = storeFile(file);
            image.setUrl("/api/images/files/" + filename);
        }
        return ImageInfoResponse.from(imageInfoRepository.save(image));
    }

    public void delete(Long id) {
        ImageInfo image = imageInfoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Image not found with id: " + id));
        deletePhysicalFile(image.getUrl());
        imageInfoRepository.deleteById(id);
    }

    public Resource loadFileAsResource(String filename) {
        try {
            Path filePath = Paths.get(uploadDir).resolve(filename).normalize();
            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists() && resource.isReadable()) {
                return resource;
            }
            throw new RuntimeException("File not found: " + filename);
        } catch (Exception e) {
            throw new RuntimeException("File not found: " + filename, e);
        }
    }

    private String storeFile(MultipartFile file) {
        try {
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }
            String original = file.getOriginalFilename();
            String ext = (original != null && original.contains("."))
                    ? original.substring(original.lastIndexOf('.'))
                    : "";
            String filename = UUID.randomUUID() + ext;
            Files.copy(file.getInputStream(), uploadPath.resolve(filename), StandardCopyOption.REPLACE_EXISTING);
            return filename;
        } catch (IOException e) {
            throw new RuntimeException("Failed to store uploaded file", e);
        }
    }

    private void deletePhysicalFile(String url) {
        if (url == null || !url.startsWith("/api/images/files/")) return;
        String filename = url.substring("/api/images/files/".length());
        try {
            Files.deleteIfExists(Paths.get(uploadDir).resolve(filename));
        } catch (IOException ignored) {
        }
    }
}
