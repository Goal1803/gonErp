package com.gonerp.common;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
public class AdminController {

    @Value("${app.upload.dir}")
    private String imageUploadDir;

    @Value("${app.upload.taskmanager}")
    private String taskUploadDir;

    @Value("${app.upload.users}")
    private String userUploadDir;

    @PostMapping("/generate-thumbnails")
    public ResponseEntity<ApiResponse<Map<String, Object>>> generateThumbnails() {
        Map<String, Object> result = new LinkedHashMap<>();

        Map<String, Integer> images = ImageUtil.generateThumbnailsForDirectory(Paths.get(imageUploadDir));
        result.put("images", images);

        Map<String, Integer> tasks = ImageUtil.generateThumbnailsForDirectory(Paths.get(taskUploadDir));
        result.put("taskmanager", tasks);

        Map<String, Integer> users = ImageUtil.generateThumbnailsForDirectory(Paths.get(userUploadDir));
        result.put("users", users);

        int totalGenerated = images.get("generated") + tasks.get("generated") + users.get("generated");
        int totalSkipped = images.get("skipped") + tasks.get("skipped") + users.get("skipped");
        int totalFailed = images.get("failed") + tasks.get("failed") + users.get("failed");
        result.put("total", Map.of("generated", totalGenerated, "skipped", totalSkipped, "failed", totalFailed));

        return ResponseEntity.ok(ApiResponse.ok(
                "Generated " + totalGenerated + " thumbnails, skipped " + totalSkipped + " (already exist), " + totalFailed + " failed",
                result));
    }
}
