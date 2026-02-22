package com.gonerp.taskmanager.model;

import com.gonerp.common.BaseModel;
import com.gonerp.taskmanager.model.enums.NotificationType;
import com.gonerp.usermanager.model.User;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "tm_notifications", indexes = {
        @Index(name = "idx_notif_recipient_read", columnList = "recipient_id, is_read"),
        @Index(name = "idx_notif_recipient_created", columnList = "recipient_id, created_at DESC")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification extends BaseModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipient_id", nullable = false)
    private User recipient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_id", nullable = false)
    private User actor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private NotificationType type;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(name = "board_id")
    private Long boardId;

    @Column(name = "card_id")
    private Long cardId;

    @Column(name = "card_name", length = 500)
    private String cardName;

    @Column(name = "is_read", nullable = false, columnDefinition = "boolean default false")
    @Builder.Default
    private boolean read = false;
}
