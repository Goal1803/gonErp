package com.gonerp.taskmanager.model.enums;

public enum NotificationType {
    CARD_MEMBER_ADDED,
    CARD_STAGE_MOVED,
    COMMENT_MENTION;

    public boolean isImportant() {
        return this == COMMENT_MENTION;
    }
}
