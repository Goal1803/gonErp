package com.gonerp.common;

import com.gonerp.config.R2StorageProperties;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FileStorageService {

    private static final Logger log = LoggerFactory.getLogger(FileStorageService.class);

    private final S3Client s3Client;
    private final R2StorageProperties r2Props;

    /**
     * Uploads file to R2, generates + uploads thumbnail, returns public URL.
     */
    public String store(MultipartFile file, String prefix) {
        String original = file.getOriginalFilename();
        String ext = (original != null && original.contains("."))
                ? original.substring(original.lastIndexOf('.'))
                : "";
        String uuid = UUID.randomUUID().toString();
        String key = prefix + "/" + uuid + ext;

        try {
            // Upload original file
            s3Client.putObject(
                    PutObjectRequest.builder()
                            .bucket(r2Props.getBucket())
                            .key(key)
                            .contentType(file.getContentType())
                            .build(),
                    RequestBody.fromInputStream(file.getInputStream(), file.getSize()));

            // Generate and upload thumbnail
            byte[] thumbBytes = ImageUtil.generateThumbnailBytes(file.getInputStream(), file.getContentType());
            if (thumbBytes != null) {
                String thumbKey = prefix + "/" + uuid + "_thumb.jpg";
                s3Client.putObject(
                        PutObjectRequest.builder()
                                .bucket(r2Props.getBucket())
                                .key(thumbKey)
                                .contentType("image/jpeg")
                                .build(),
                        RequestBody.fromBytes(thumbBytes));
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to upload file to R2", e);
        }

        return r2Props.getPublicUrl() + "/" + key;
    }

    /**
     * Deletes file and its thumbnail from R2.
     */
    public void delete(String publicUrl) {
        if (publicUrl == null || !publicUrl.startsWith(r2Props.getPublicUrl())) return;
        String key = publicUrl.substring(r2Props.getPublicUrl().length() + 1);

        try {
            s3Client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(r2Props.getBucket()).key(key).build());

            // Delete thumbnail
            String thumbKey = thumbnailKey(key);
            if (thumbKey != null) {
                s3Client.deleteObject(DeleteObjectRequest.builder()
                        .bucket(r2Props.getBucket()).key(thumbKey).build());
            }
        } catch (Exception e) {
            log.warn("Failed to delete R2 object {}: {}", key, e.getMessage());
        }
    }

    /**
     * Converts a public URL to its thumbnail variant.
     */
    public String thumbnailUrl(String url) {
        if (url == null) return null;
        int dot = url.lastIndexOf('.');
        return dot == -1 ? url : url.substring(0, dot) + "_thumb.jpg";
    }

    /**
     * Downloads a file from R2 by key and returns it as a Resource.
     */
    public Resource loadFromR2(String key) {
        try {
            ResponseInputStream<GetObjectResponse> response = s3Client.getObject(
                    GetObjectRequest.builder()
                            .bucket(r2Props.getBucket())
                            .key(key)
                            .build());
            return new InputStreamResource(response);
        } catch (Exception e) {
            log.warn("Failed to load from R2 {}: {}", key, e.getMessage());
            return null;
        }
    }

    private String thumbnailKey(String key) {
        int dot = key.lastIndexOf('.');
        return dot == -1 ? null : key.substring(0, dot) + "_thumb.jpg";
    }
}
