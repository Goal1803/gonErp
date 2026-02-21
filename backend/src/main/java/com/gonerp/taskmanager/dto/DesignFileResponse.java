package com.gonerp.taskmanager.dto;

import com.gonerp.taskmanager.model.DesignFile;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class DesignFileResponse {
    private Long id;
    private String name;
    private String url;
    private String fileType;
    private String fileCategory;
    private LocalDateTime createdAt;

    public static DesignFileResponse from(DesignFile f) {
        return DesignFileResponse.builder()
                .id(f.getId())
                .name(f.getName())
                .url(f.getUrl())
                .fileType(f.getFileType())
                .fileCategory(f.getFileCategory().name())
                .createdAt(f.getCreatedAt())
                .build();
    }
}
