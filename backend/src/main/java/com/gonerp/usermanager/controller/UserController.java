package com.gonerp.usermanager.controller;

import com.gonerp.common.ApiResponse;
import com.gonerp.config.R2StorageProperties;
import com.gonerp.usermanager.dto.UserRequest;
import com.gonerp.usermanager.dto.UserResponse;
import com.gonerp.usermanager.model.enums.UserStatus;
import com.gonerp.usermanager.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
public class UserController {

    private final UserService userService;
    private final R2StorageProperties r2Props;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<UserResponse>>> findAll(
            @RequestParam(required = false) UserStatus status,
            @RequestParam(required = false) Long organizationId,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();
        PageRequest pageable = PageRequest.of(page, size, sort);

        return ResponseEntity.ok(ApiResponse.ok(userService.findAll(status, organizationId, search, pageable)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> findById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(userService.findById(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<UserResponse>> create(@Valid @RequestBody UserRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("User created successfully", userService.create(request)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> update(@PathVariable Long id,
                                                            @Valid @RequestBody UserRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("User updated successfully", userService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> softDelete(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok("User deleted successfully", userService.softDelete(id)));
    }

    @PostMapping("/{id}/avatar")
    public ResponseEntity<ApiResponse<UserResponse>> uploadAvatar(@PathVariable Long id,
                                                                   @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(ApiResponse.ok("Avatar uploaded", userService.uploadAvatar(id, file)));
    }

    @DeleteMapping("/{id}/avatar")
    public ResponseEntity<ApiResponse<UserResponse>> deleteAvatar(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok("Avatar removed", userService.deleteAvatar(id)));
    }

    @GetMapping("/files/{filename:.+}")
    @PreAuthorize("permitAll()")
    public ResponseEntity<?> serveFile(
            @PathVariable String filename,
            @RequestParam(defaultValue = "false") boolean thumb) {
        String resolvedFilename = filename;
        if (thumb) {
            String baseName = filename.contains(".")
                    ? filename.substring(0, filename.lastIndexOf('.'))
                    : filename;
            resolvedFilename = baseName + "_thumb.jpg";
        }

        // Try local disk first (backward compat)
        Resource resource = userService.loadFileAsResource(resolvedFilename);
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

        // Fallback: redirect to R2
        String r2Url = r2Props.getPublicUrl() + "/users/" + resolvedFilename;
        return ResponseEntity.status(HttpStatus.MOVED_PERMANENTLY)
                .location(URI.create(r2Url))
                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=31536000, immutable")
                .build();
    }
}
