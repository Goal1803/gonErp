package com.gonerp.taskmanager.dto;

import com.gonerp.taskmanager.model.DesignMockup;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class DesignMockupResponse {
    private Long id;
    private String name;
    private String url;
    private String fileType;
    private boolean mainMockup;
    private LocalDateTime createdAt;

    public static DesignMockupResponse from(DesignMockup m) {
        return DesignMockupResponse.builder()
                .id(m.getId())
                .name(m.getName())
                .url(m.getUrl())
                .fileType(m.getFileType())
                .mainMockup(m.isMainMockup())
                .createdAt(m.getCreatedAt())
                .build();
    }
}
