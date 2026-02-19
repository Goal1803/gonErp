package com.gonerp.imagemanager.dto;

import com.gonerp.imagemanager.model.ImageInfo;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ImageInfoResponse {
    private Long id;
    private String name;
    private String url;
    private LocalDateTime createdAt;
    private String createdBy;
    private LocalDateTime lastUpdatedAt;
    private String lastUpdatedBy;

    public static ImageInfoResponse from(ImageInfo image) {
        return ImageInfoResponse.builder()
                .id(image.getId())
                .name(image.getName())
                .url(image.getUrl())
                .createdAt(image.getCreatedAt())
                .createdBy(image.getCreatedBy())
                .lastUpdatedAt(image.getLastUpdatedAt())
                .lastUpdatedBy(image.getLastUpdatedBy())
                .build();
    }
}
