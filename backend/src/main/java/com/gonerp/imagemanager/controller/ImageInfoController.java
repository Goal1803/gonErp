package com.gonerp.imagemanager.controller;

import com.gonerp.common.ApiResponse;
import com.gonerp.config.R2StorageProperties;
import com.gonerp.imagemanager.dto.ImageInfoResponse;
import com.gonerp.imagemanager.service.ImageInfoService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;

@RestController
@RequestMapping("/api/images")
@RequiredArgsConstructor
public class ImageInfoController {

    private final ImageInfoService imageInfoService;
    private final R2StorageProperties r2Props;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<ImageInfoResponse>>> findAll(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();
        PageRequest pageable = PageRequest.of(page, size, sort);

        return ResponseEntity.ok(ApiResponse.ok(imageInfoService.findAll(search, pageable)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ImageInfoResponse>> findById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(imageInfoService.findById(id)));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<ImageInfoResponse>> create(
            @RequestParam(value = "name", required = false) String name,
            @RequestParam("file") MultipartFile file) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Image uploaded successfully", imageInfoService.create(name, file)));
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<ImageInfoResponse>> update(
            @PathVariable Long id,
            @RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "file", required = false) MultipartFile file) {
        return ResponseEntity.ok(ApiResponse.ok("Image updated successfully", imageInfoService.update(id, name, file)));
    }

    @GetMapping("/files/{filename:.+}")
    public ResponseEntity<?> serveFile(
            @PathVariable String filename,
            @RequestParam(defaultValue = "false") boolean thumb) {
        // Resolve thumbnail filename if requested
        String resolvedFilename = filename;
        if (thumb) {
            String baseName = filename.contains(".")
                    ? filename.substring(0, filename.lastIndexOf('.'))
                    : filename;
            resolvedFilename = baseName + "_thumb.jpg";
        }

        // Try local disk first (backward compat)
        Resource resource = imageInfoService.loadFileAsResource(resolvedFilename);
        if (resource != null) {
            // If thumb was requested but thumbnail doesn't exist, fall back to original
            if (thumb) {
                Resource originalResource = imageInfoService.loadFileAsResource(filename);
                if (originalResource == null) {
                    resource = null; // will fall through to R2 redirect
                }
            }
            if (resource != null) {
                String contentType = "application/octet-stream";
                try {
                    contentType = Files.probeContentType(Paths.get(resolvedFilename));
                    if (contentType == null) contentType = "application/octet-stream";
                } catch (IOException ignored) {}
                return ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(contentType))
                        .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                        .header(HttpHeaders.CACHE_CONTROL, "public, max-age=31536000, immutable")
                        .body(resource);
            }
        }

        // Fallback: redirect to R2
        String r2Url = r2Props.getPublicUrl() + "/images/" + resolvedFilename;
        return ResponseEntity.status(HttpStatus.MOVED_PERMANENTLY)
                .location(URI.create(r2Url))
                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=31536000, immutable")
                .build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        imageInfoService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok("Image deleted successfully", null));
    }
}
