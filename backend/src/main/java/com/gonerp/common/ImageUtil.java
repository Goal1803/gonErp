package com.gonerp.common;

import net.coobird.thumbnailator.Thumbnails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.stream.Stream;

public final class ImageUtil {

    private static final Logger log = LoggerFactory.getLogger(ImageUtil.class);
    private static final int THUMB_MAX_WIDTH = 400;
    private static final double THUMB_QUALITY = 0.8;

    private ImageUtil() {}

    public static void generateThumbnail(Path originalFile, String contentType) {
        if (contentType == null || !contentType.startsWith("image/")) return;
        // Skip non-rasterizable formats
        String lower = contentType.toLowerCase();
        if (lower.contains("psd") || lower.contains("photoshop") || lower.contains("svg")) return;

        try {
            String filename = originalFile.getFileName().toString();
            String baseName = filename.contains(".")
                    ? filename.substring(0, filename.lastIndexOf('.'))
                    : filename;
            Path thumbPath = originalFile.getParent().resolve(baseName + "_thumb.jpg");

            Thumbnails.of(originalFile.toFile())
                    .width(THUMB_MAX_WIDTH)
                    .outputFormat("jpg")
                    .outputQuality(THUMB_QUALITY)
                    .toFile(thumbPath.toFile());
        } catch (Exception e) {
            log.warn("Thumbnail generation failed for {}: {}", originalFile.getFileName(), e.getMessage());
        }
    }

    private static final java.util.Set<String> IMAGE_EXTENSIONS = java.util.Set.of(
            ".jpg", ".jpeg", ".png", ".gif", ".bmp", ".webp", ".tiff", ".tif"
    );

    public static Map<String, Integer> generateThumbnailsForDirectory(Path directory) {
        int generated = 0;
        int skipped = 0;
        int failed = 0;

        if (!Files.isDirectory(directory)) {
            return Map.of("generated", 0, "skipped", 0, "failed", 0);
        }

        try (Stream<Path> files = Files.list(directory)) {
            for (Path file : files.toList()) {
                String name = file.getFileName().toString();
                if (name.contains("_thumb.")) {
                    continue;
                }
                String ext = name.contains(".") ? name.substring(name.lastIndexOf('.')).toLowerCase() : "";
                if (!IMAGE_EXTENSIONS.contains(ext)) {
                    continue;
                }
                String baseName = name.substring(0, name.lastIndexOf('.'));
                Path thumbPath = directory.resolve(baseName + "_thumb.jpg");
                if (Files.exists(thumbPath)) {
                    skipped++;
                    continue;
                }
                try {
                    Thumbnails.of(file.toFile())
                            .width(THUMB_MAX_WIDTH)
                            .outputFormat("jpg")
                            .outputQuality(THUMB_QUALITY)
                            .toFile(thumbPath.toFile());
                    generated++;
                } catch (Exception e) {
                    log.warn("Thumbnail generation failed for {}: {}", name, e.getMessage());
                    failed++;
                }
            }
        } catch (IOException e) {
            log.error("Failed to scan directory {}: {}", directory, e.getMessage());
        }

        return Map.of("generated", generated, "skipped", skipped, "failed", failed);
    }

    public static byte[] generateThumbnailBytes(InputStream inputStream, String contentType) {
        if (contentType == null || !contentType.startsWith("image/")) return null;
        String lower = contentType.toLowerCase();
        if (lower.contains("psd") || lower.contains("photoshop") || lower.contains("svg")) return null;

        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            Thumbnails.of(inputStream)
                    .width(THUMB_MAX_WIDTH)
                    .outputFormat("jpg")
                    .outputQuality(THUMB_QUALITY)
                    .toOutputStream(baos);
            return baos.toByteArray();
        } catch (Exception e) {
            log.warn("In-memory thumbnail generation failed: {}", e.getMessage());
            return null;
        }
    }

    public static void deleteThumbnail(Path directory, String filename) {
        if (filename == null) return;
        String baseName = filename.contains(".")
                ? filename.substring(0, filename.lastIndexOf('.'))
                : filename;
        try {
            Files.deleteIfExists(directory.resolve(baseName + "_thumb.jpg"));
        } catch (Exception e) {
            log.warn("Failed to delete thumbnail for {}: {}", filename, e.getMessage());
        }
    }
}
