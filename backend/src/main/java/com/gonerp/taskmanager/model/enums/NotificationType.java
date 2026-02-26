package com.gonerp.taskmanager.model.enums;

public enum NotificationType {
    CARD_MEMBER_ADDED,
    CARD_STAGE_MOVED,
    COMMENT_MENTION,
    DAY_OFF_REQUESTED,
    DAY_OFF_APPROVED,
    DAY_OFF_DENIED,
    AUTO_CHECKOUT_REMINDER;

    public boolean isImportant() {
        return this == COMMENT_MENTION || this == DAY_OFF_APPROVED || this == DAY_OFF_DENIED || this == AUTO_CHECKOUT_REMINDER;
    }
}
