package com.gonerp.taskmanager.dto;

import com.gonerp.taskmanager.model.Card;
import com.gonerp.taskmanager.model.enums.CardStatus;
import lombok.Builder;
import lombok.Data;

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
    private List<LabelResponse> labels;
    private List<TypeResponse> types;
    private List<UserSummaryResponse> members;
    private int commentCount;
    private int attachmentCount;

    public static CardSummaryResponse from(Card card) {
        return CardSummaryResponse.builder()
                .id(card.getId())
                .name(card.getName())
                .status(card.getStatus())
                .stage(card.getStage())
                .position(card.getPosition())
                .mainImageUrl(card.getMainImageUrl())
                .labels(card.getLabels().stream().map(LabelResponse::from).toList())
                .types(card.getTypes().stream().map(TypeResponse::from).toList())
                .members(card.getMembers().stream()
                        .map(m -> UserSummaryResponse.from(m.getUser())).toList())
                .commentCount(card.getComments().size())
                .attachmentCount(card.getAttachments().size())
                .build();
    }
}
