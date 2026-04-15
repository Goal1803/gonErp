package com.gonerp.worktime.event;

import com.gonerp.taskmanager.dto.NotificationResponse;
import com.gonerp.taskmanager.model.Notification;
import com.gonerp.taskmanager.model.enums.NotificationType;
import com.gonerp.taskmanager.repository.NotificationRepository;
import com.gonerp.usermanager.model.User;
import com.gonerp.usermanager.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class WorkTimeNotificationListener {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @Async
    @TransactionalEventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handle(WorkTimeNotificationEvent event) {
        NotificationType type;
        try {
            type = NotificationType.valueOf(event.getType());
        } catch (IllegalArgumentException e) {
            log.warn("Unknown worktime notification type: {}", event.getType());
            return;
        }

        User actor = userRepository.findById(event.getActorId()).orElse(null);
        if (actor == null) {
            log.warn("WorkTime notification actor not found: {}", event.getActorId());
            return;
        }

        for (Long recipientId : event.getRecipientIds()) {
            User recipient = userRepository.findById(recipientId).orElse(null);
            if (recipient == null) continue;

            Notification n = Notification.builder()
                    .recipient(recipient)
                    .actor(actor)
                    .type(type)
                    .message(event.getMessage())
                    .build();
            n = notificationRepository.save(n);

            try {
                messagingTemplate.convertAndSendToUser(
                        recipient.getUserName(), "/queue/notifications", NotificationResponse.from(n));
            } catch (Exception ex) {
                log.warn("Failed to push notification to {}: {}", recipient.getUserName(), ex.getMessage());
            }
        }
    }
}
