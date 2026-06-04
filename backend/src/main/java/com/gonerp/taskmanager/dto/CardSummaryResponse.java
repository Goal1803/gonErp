package com.gonerp.taskmanager.dto;

import com.gonerp.taskmanager.model.Card;
import com.gonerp.taskmanager.model.enums.CardGenType;
import com.gonerp.taskmanager.model.enums.CardStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class CardSummaryResponse {
    private Long id;
    private String name;
    private CardStatus status;
    private String stage;
    private int position;
    private String mainImageUrl;
    private String sku;
    private CardGenType genType;
    private List<LabelResponse> labels;
    private List<TypeResponse> types;
    private List<UserSummaryResponse> members;
    private int commentCount;
    private int attachmentCount;
    private LocalDateTime createdAt;
    private boolean archived;
    private LocalDateTime archivedAt;

    // Convenience overload for single-card paths (loads comment/attachment
    // collections to count). For board/column listing use the overload below
    // with pre-computed batch counts to avoid loading those collections.
    public static CardSummaryResponse from(Card card) {
        return from(card, card.getComments().size(), card.getAttachments().size());
    }

    public static CardSummaryResponse from(Card card, int commentCount, int attachmentCount) {
        return CardSummaryResponse.builder()
                .id(card.getId())
                .name(card.getName())
                .status(card.getStatus())
                .stage(card.getStage())
                .position(card.getPosition())
                .mainImageUrl(card.getMainImageUrl())
                .sku(card.getSku())
                .genType(card.getGenType())
                .labels(card.getLabels().stream().map(LabelResponse::from).toList())
                .types(card.getTypes().stream().map(TypeResponse::from).toList())
                .members(card.getMembers().stream()
                        .map(m -> UserSummaryResponse.from(m.getUser())).toList())
                .commentCount(commentCount)
                .attachmentCount(attachmentCount)
                .createdAt(card.getCreatedAt())
                .archived(card.isArchived())
                .archivedAt(card.getArchivedAt())
                .build();
    }
}
