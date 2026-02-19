package com.gonerp.taskmanager.dto;

import com.gonerp.taskmanager.model.Card;
import com.gonerp.taskmanager.model.enums.CardStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class CardDetailResponse {
    private Long id;
    private String name;
    private String description;
    private CardStatus status;
    private String stage;
    private int position;
    private String mainImageUrl;
    private Long columnId;
    private Long boardId;
    private List<LabelResponse> labels;
    private List<UserSummaryResponse> members;
    private List<AttachmentResponse> attachments;
    private List<CommentResponse> comments;
    private List<ActivityResponse> activities;
    private LocalDateTime createdAt;
    private String createdBy;
    private LocalDateTime lastUpdatedAt;
    private String lastUpdatedBy;

    public static CardDetailResponse from(Card card) {
        return CardDetailResponse.builder()
                .id(card.getId())
                .name(card.getName())
                .description(card.getDescription())
                .status(card.getStatus())
                .stage(card.getStage())
                .position(card.getPosition())
                .mainImageUrl(card.getMainImageUrl())
                .columnId(card.getColumn().getId())
                .boardId(card.getColumn().getBoard().getId())
                .labels(card.getLabels().stream().map(LabelResponse::from).toList())
                .members(card.getMembers().stream()
                        .map(m -> UserSummaryResponse.from(m.getUser())).toList())
                .attachments(card.getAttachments().stream().map(AttachmentResponse::from).toList())
                .comments(card.getComments().stream().map(CommentResponse::from).toList())
                .activities(card.getActivities().stream().map(ActivityResponse::from).toList())
                .createdAt(card.getCreatedAt())
                .createdBy(card.getCreatedBy())
                .lastUpdatedAt(card.getLastUpdatedAt())
                .lastUpdatedBy(card.getLastUpdatedBy())
                .build();
    }
}
