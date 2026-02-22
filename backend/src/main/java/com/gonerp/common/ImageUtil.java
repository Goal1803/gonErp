package com.gonerp.common;

import net.coobird.thumbnailator.Thumbnails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;

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
