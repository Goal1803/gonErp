package com.gonerp.worktime.event;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Set;

@Data
@AllArgsConstructor
public class WorkTimeNotificationEvent {
    private Long actorId;
    private Set<Long> recipientIds;
    private String type;
    private String message;
}
