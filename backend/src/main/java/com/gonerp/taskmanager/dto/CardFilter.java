package com.gonerp.taskmanager.dto;

import com.gonerp.taskmanager.model.enums.CardStatus;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

/**
 * Server-side card filter for board/column listing. All client fields are
 * optional; an absent/empty field means "no constraint". {@code restrictToMemberUserId}
 * is set by the service (never from the client) to enforce per-card visibility
 * for non-privileged users.
 */
@Data
public class CardFilter {
    private List<Long> memberIds;
    private List<Long> labelIds;
    private List<Long> typeIds;
    private List<CardStatus> statuses;
    private LocalDate dateFrom;
    private LocalDate dateTo;
    private String q;

    private Long restrictToMemberUserId;
}
