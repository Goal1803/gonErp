package com.gonerp.imagemanager.service;

import com.gonerp.common.FileStorageService;
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

import java.nio.file.Path;
import java.nio.file.Paths;

@Service
@RequiredArgsConstructor
@Transactional
public class ImageInfoService {

    private final ImageInfoRepository imageInfoRepository;
    private final FileStorageService fileStorageService;

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
        String url = fileStorageService.store(file, "images");
        String resolvedName = (name != null && !name.isBlank()) ? name : file.getOriginalFilename();
        ImageInfo image = ImageInfo.builder()
                .name(resolvedName)
                .url(url)
                .build();
        return ImageInfoResponse.from(imageInfoRepository.save(image));
    }

    public ImageInfoResponse update(Long id, String name, MultipartFile file) {
        ImageInfo image = imageInfoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Image not found with id: " + id));
        image.setName(name);
        if (file != null && !file.isEmpty()) {
            fileStorageService.delete(image.getUrl());
            String url = fileStorageService.store(file, "images");
            image.setUrl(url);
        }
        return ImageInfoResponse.from(imageInfoRepository.save(image));
    }

    public void delete(Long id) {
        ImageInfo image = imageInfoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Image not found with id: " + id));
        fileStorageService.delete(image.getUrl());
        imageInfoRepository.deleteById(id);
    }

    /**
     * Legacy: loads file from local disk (for backward-compat redirect fallback).
     */
    public Resource loadFileAsResource(String filename) {
        try {
            Path filePath = Paths.get(uploadDir).resolve(filename).normalize();
            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists() && resource.isReadable()) {
                return resource;
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }
}
