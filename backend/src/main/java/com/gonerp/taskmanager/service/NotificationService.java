package com.gonerp.taskmanager.service;

import com.gonerp.taskmanager.dto.NotificationResponse;
import com.gonerp.taskmanager.event.NotificationEvent;
import com.gonerp.taskmanager.model.Notification;
import com.gonerp.taskmanager.model.enums.NotificationType;
import com.gonerp.taskmanager.repository.NotificationRepository;
import com.gonerp.usermanager.model.User;
import com.gonerp.usermanager.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @Async
    @TransactionalEventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleNotificationEvent(NotificationEvent event) {
        User actor = userRepository.findById(event.getActorId()).orElse(null);
        if (actor == null) {
            log.warn("Notification actor not found: {}", event.getActorId());
            return;
        }

        for (Long recipientId : event.getRecipientIds()) {
            User recipient = userRepository.findById(recipientId).orElse(null);
            if (recipient == null) continue;

            Notification notification = Notification.builder()
                    .recipient(recipient)
                    .actor(actor)
                    .type(event.getType())
                    .message(event.getMessage())
                    .boardId(event.getBoardId())
                    .cardId(event.getCardId())
                    .cardName(event.getCardName())
                    .build();
            notification = notificationRepository.save(notification);

            NotificationResponse dto = NotificationResponse.from(notification);
            messagingTemplate.convertAndSendToUser(
                    recipient.getUserName(), "/queue/notifications", dto);
        }
    }

    @Transactional(readOnly = true)
    public Page<NotificationResponse> getNotifications(Long recipientId, int page, int size) {
        return notificationRepository
                .findByRecipientIdOrderByCreatedAtDesc(recipientId, PageRequest.of(page, size))
                .map(NotificationResponse::from);
    }

    @Transactional(readOnly = true)
    public Page<NotificationResponse> getImportantNotifications(Long recipientId, int page, int size) {
        List<NotificationType> importantTypes = List.of(NotificationType.COMMENT_MENTION);
        return notificationRepository
                .findByRecipientIdAndTypeInOrderByCreatedAtDesc(
                        recipientId, importantTypes, PageRequest.of(page, size))
                .map(NotificationResponse::from);
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(Long recipientId) {
        return notificationRepository.countByRecipientIdAndReadFalse(recipientId);
    }

    @Transactional
    public void markAsRead(Long notificationId) {
        notificationRepository.findById(notificationId).ifPresent(n -> {
            n.setRead(true);
            notificationRepository.save(n);
        });
    }

    @Transactional
    public void markAllAsRead(Long recipientId) {
        notificationRepository.markAllAsRead(recipientId);
    }
}
