package com.gonerp.taskmanager.dto;

import com.gonerp.taskmanager.model.Notification;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class NotificationResponse {
    private Long id;
    private String type;
    private boolean important;
    private String message;
    private Long boardId;
    private Long cardId;
    private String cardName;
    private boolean read;
    private UserSummaryResponse actor;
    private LocalDateTime createdAt;

    public static NotificationResponse from(Notification n) {
        return NotificationResponse.builder()
                .id(n.getId())
                .type(n.getType().name())
                .important(n.getType().isImportant())
                .message(n.getMessage())
                .boardId(n.getBoardId())
                .cardId(n.getCardId())
                .cardName(n.getCardName())
                .read(n.isRead())
                .actor(UserSummaryResponse.from(n.getActor()))
                .createdAt(n.getCreatedAt())
                .build();
    }
}
