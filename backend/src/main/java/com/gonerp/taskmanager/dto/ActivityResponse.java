package com.gonerp.taskmanager.dto;

import com.gonerp.taskmanager.model.CardActivity;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ActivityResponse {
    private Long id;
    private String action;
    private String actorName;
    private LocalDateTime createdAt;

    public static ActivityResponse from(CardActivity activity) {
        return ActivityResponse.builder()
                .id(activity.getId())
                .action(activity.getAction())
                .actorName(activity.getActor().getUserName())
                .createdAt(activity.getCreatedAt())
                .build();
    }
}
