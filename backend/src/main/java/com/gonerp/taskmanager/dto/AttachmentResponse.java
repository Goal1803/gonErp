package com.gonerp.taskmanager.dto;

import com.gonerp.taskmanager.model.CardAttachment;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class AttachmentResponse {
    private Long id;
    private String name;
    private String url;
    private String fileType;
    private LocalDateTime createdAt;
    private String createdBy;

    public static AttachmentResponse from(CardAttachment att) {
        return AttachmentResponse.builder()
                .id(att.getId())
                .name(att.getName())
                .url(att.getUrl())
                .fileType(att.getFileType())
                .createdAt(att.getCreatedAt())
                .createdBy(att.getCreatedBy())
                .build();
    }
}
