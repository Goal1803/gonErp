package com.gonerp.taskmanager.dto;

import com.gonerp.taskmanager.model.CardLink;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class LinkResponse {
    private Long id;
    private String url;
    private String title;
    private LocalDateTime createdAt;
    private String createdBy;

    public static LinkResponse from(CardLink link) {
        return LinkResponse.builder()
                .id(link.getId())
                .url(link.getUrl())
                .title(link.getTitle())
                .createdAt(link.getCreatedAt())
                .createdBy(link.getCreatedBy())
                .build();
    }
}
