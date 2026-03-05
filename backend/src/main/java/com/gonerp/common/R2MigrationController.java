package com.gonerp.common;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
public class R2MigrationController {

    private final R2MigrationService r2MigrationService;

    @PostMapping("/migrate-to-r2")
    public ResponseEntity<ApiResponse<Map<String, Object>>> migrateToR2() {
        Map<String, Object> result = r2MigrationService.migrate();
        return ResponseEntity.ok(ApiResponse.ok("Migration completed", result));
    }

    @PostMapping("/regenerate-thumbnails")
    public ResponseEntity<ApiResponse<Map<String, Object>>> regenerateThumbnails() {
        Map<String, Object> result = r2MigrationService.regenerateMissingThumbnails();
        return ResponseEntity.ok(ApiResponse.ok("Thumbnail regeneration completed", result));
    }
}
