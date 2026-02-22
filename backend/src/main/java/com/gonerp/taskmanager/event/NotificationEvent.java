package com.gonerp.taskmanager.event;

import com.gonerp.taskmanager.model.enums.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationEvent {
    private NotificationType type;
    private Long actorId;
    private Set<Long> recipientIds;
    private String message;
    private Long boardId;
    private Long cardId;
    private String cardName;
}
