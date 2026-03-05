package com.gonerp.common;

import com.gonerp.config.R2StorageProperties;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.S3Object;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class R2MigrationService {

    private static final Logger log = LoggerFactory.getLogger(R2MigrationService.class);

    private final S3Client s3Client;
    private final R2StorageProperties r2Props;
    private final JdbcTemplate jdbcTemplate;

    @Value("${app.upload.dir}")
    private String imagesDir;

    @Value("${app.upload.taskmanager}")
    private String taskmanagerDir;

    @Value("${app.upload.users}")
    private String usersDir;

    public Map<String, Object> migrate() {
        Map<String, Object> result = new LinkedHashMap<>();

        // Phase 1: Upload files to R2
        int imagesUploaded = uploadDirectory(Paths.get(imagesDir), "images");
        int taskmanagerUploaded = uploadDirectory(Paths.get(taskmanagerDir), "taskmanager");
        int usersUploaded = uploadDirectory(Paths.get(usersDir), "users");

        result.put("filesUploaded", Map.of(
                "images", imagesUploaded,
                "taskmanager", taskmanagerUploaded,
                "users", usersUploaded
        ));

        // Phase 2: Update database URLs
        String publicUrl = r2Props.getPublicUrl();
        Map<String, Integer> dbUpdates = new LinkedHashMap<>();

        dbUpdates.put("users", jdbcTemplate.update(
                "UPDATE users SET avatar_url = REPLACE(avatar_url, '/api/users/files/', ?) WHERE avatar_url LIKE '/api/users/files/%'",
                publicUrl + "/users/"));

        dbUpdates.put("image_info", jdbcTemplate.update(
                "UPDATE image_info SET url = REPLACE(url, '/api/images/files/', ?) WHERE url LIKE '/api/images/files/%'",
                publicUrl + "/images/"));

        dbUpdates.put("tm_cards", jdbcTemplate.update(
                "UPDATE tm_cards SET main_image_url = REPLACE(main_image_url, '/api/tasks/files/', ?) WHERE main_image_url LIKE '/api/tasks/files/%'",
                publicUrl + "/taskmanager/"));

        dbUpdates.put("tm_card_attachments", jdbcTemplate.update(
                "UPDATE tm_card_attachments SET url = REPLACE(url, '/api/tasks/files/', ?) WHERE url LIKE '/api/tasks/files/%'",
                publicUrl + "/taskmanager/"));

        dbUpdates.put("tm_design_files", jdbcTemplate.update(
                "UPDATE tm_design_files SET url = REPLACE(url, '/api/tasks/files/', ?) WHERE url LIKE '/api/tasks/files/%'",
                publicUrl + "/taskmanager/"));

        dbUpdates.put("tm_design_mockups", jdbcTemplate.update(
                "UPDATE tm_design_mockups SET url = REPLACE(url, '/api/tasks/files/', ?) WHERE url LIKE '/api/tasks/files/%'",
                publicUrl + "/taskmanager/"));

        dbUpdates.put("tm_card_comments", jdbcTemplate.update(
                "UPDATE tm_card_comments SET image_urls = REPLACE(image_urls, '/api/tasks/files/', ?) WHERE image_urls LIKE '%/api/tasks/files/%'",
                publicUrl + "/taskmanager/"));

        result.put("databaseUpdates", dbUpdates);
        return result;
    }

    private int uploadDirectory(Path directory, String prefix) {
        if (!Files.isDirectory(directory)) {
            log.warn("Directory not found: {}", directory);
            return 0;
        }

        AtomicInteger uploaded = new AtomicInteger(0);
        ExecutorService executor = Executors.newFixedThreadPool(4);

        try (Stream<Path> files = Files.list(directory)) {
            files.forEach(file -> executor.submit(() -> {
                try {
                    String filename = file.getFileName().toString();
                    String key = prefix + "/" + filename;

                    // Skip if already exists in R2
                    if (existsInR2(key)) {
                        log.debug("Already in R2, skipping: {}", key);
                        return;
                    }

                    String contentType = Files.probeContentType(file);
                    if (contentType == null) contentType = "application/octet-stream";

                    s3Client.putObject(
                            PutObjectRequest.builder()
                                    .bucket(r2Props.getBucket())
                                    .key(key)
                                    .contentType(contentType)
                                    .build(),
                            RequestBody.fromFile(file));

                    uploaded.incrementAndGet();
                    if (uploaded.get() % 100 == 0) {
                        log.info("Uploaded {} files to {}/", uploaded.get(), prefix);
                    }
                } catch (Exception e) {
                    log.error("Failed to upload {}: {}", file.getFileName(), e.getMessage());
                }
            }));
        } catch (IOException e) {
            log.error("Failed to scan directory {}: {}", directory, e.getMessage());
        }

        executor.shutdown();
        try {
            executor.awaitTermination(2, java.util.concurrent.TimeUnit.HOURS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        log.info("Uploaded {} files from {} to R2 prefix '{}'", uploaded.get(), directory, prefix);
        return uploaded.get();
    }

    private static final Set<String> IMAGE_EXTENSIONS = Set.of(
            ".jpg", ".jpeg", ".png", ".gif", ".bmp", ".webp", ".tiff", ".tif"
    );

    /**
     * Scans R2 for image files missing a _thumb.jpg and generates them.
     */
    public Map<String, Object> regenerateMissingThumbnails() {
        String[] prefixes = {"images", "taskmanager", "users"};
        Map<String, Object> result = new LinkedHashMap<>();
        int totalGenerated = 0;
        int totalSkipped = 0;
        int totalFailed = 0;

        ExecutorService executor = Executors.newFixedThreadPool(4);

        for (String prefix : prefixes) {
            AtomicInteger generated = new AtomicInteger(0);
            AtomicInteger skipped = new AtomicInteger(0);
            AtomicInteger failed = new AtomicInteger(0);

            // Collect all existing keys in this prefix to check for thumbs
            Set<String> existingKeys = java.util.concurrent.ConcurrentHashMap.newKeySet();
            String continuationToken = null;

            do {
                var reqBuilder = ListObjectsV2Request.builder()
                        .bucket(r2Props.getBucket())
                        .prefix(prefix + "/");
                if (continuationToken != null) {
                    reqBuilder.continuationToken(continuationToken);
                }
                var response = s3Client.listObjectsV2(reqBuilder.build());
                for (S3Object obj : response.contents()) {
                    existingKeys.add(obj.key());
                }
                continuationToken = response.isTruncated() ? response.nextContinuationToken() : null;
            } while (continuationToken != null);

            // Find image files missing thumbnails
            for (String key : existingKeys) {
                if (key.contains("_thumb.")) continue;

                String ext = key.contains(".") ? key.substring(key.lastIndexOf('.')).toLowerCase() : "";
                if (!IMAGE_EXTENSIONS.contains(ext)) continue;

                String baseName = key.substring(0, key.lastIndexOf('.'));
                String thumbKey = baseName + "_thumb.jpg";
                if (existingKeys.contains(thumbKey)) {
                    skipped.incrementAndGet();
                    continue;
                }

                executor.submit(() -> {
                    try {
                        ResponseInputStream<GetObjectResponse> obj = s3Client.getObject(
                                GetObjectRequest.builder()
                                        .bucket(r2Props.getBucket())
                                        .key(key)
                                        .build());

                        String contentType = obj.response().contentType();
                        byte[] fileBytes = obj.readAllBytes();
                        byte[] thumbBytes = ImageUtil.generateThumbnailBytes(
                                new ByteArrayInputStream(fileBytes), contentType);

                        if (thumbBytes != null) {
                            s3Client.putObject(
                                    PutObjectRequest.builder()
                                            .bucket(r2Props.getBucket())
                                            .key(thumbKey)
                                            .contentType("image/jpeg")
                                            .build(),
                                    RequestBody.fromBytes(thumbBytes));
                            generated.incrementAndGet();
                            log.info("Generated thumbnail: {}", thumbKey);
                        } else {
                            failed.incrementAndGet();
                            log.warn("Could not generate thumbnail for: {}", key);
                        }
                    } catch (Exception e) {
                        failed.incrementAndGet();
                        log.error("Failed to generate thumbnail for {}: {}", key, e.getMessage());
                    }
                });
            }

            executor.shutdown();
            try {
                executor.awaitTermination(1, java.util.concurrent.TimeUnit.HOURS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            executor = Executors.newFixedThreadPool(4); // fresh pool for next prefix

            result.put(prefix, Map.of(
                    "generated", generated.get(),
                    "skipped", skipped.get(),
                    "failed", failed.get()
            ));
            totalGenerated += generated.get();
            totalSkipped += skipped.get();
            totalFailed += failed.get();
        }

        result.put("total", Map.of(
                "generated", totalGenerated,
                "skipped", totalSkipped,
                "failed", totalFailed
        ));

        log.info("Thumbnail regeneration complete: generated={}, skipped={}, failed={}",
                totalGenerated, totalSkipped, totalFailed);
        return result;
    }

    private boolean existsInR2(String key) {
        try {
            s3Client.headObject(HeadObjectRequest.builder()
                    .bucket(r2Props.getBucket())
                    .key(key)
                    .build());
            return true;
        } catch (NoSuchKeyException e) {
            return false;
        }
    }
}
