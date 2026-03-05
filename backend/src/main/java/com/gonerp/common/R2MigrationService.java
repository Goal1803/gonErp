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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;
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
