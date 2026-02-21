package com.gonerp.taskmanager.dto;

import com.gonerp.taskmanager.model.DesignDetail;
import com.gonerp.taskmanager.model.DesignMockup;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class DesignSummaryResponse {
    private Long id;
    private Long cardId;
    private String cardName;
    private Long boardId;
    private String boardName;
    private boolean boardActive;
    private String mainMockupUrl;
    private String stage;
    private String status;
    private String name;
    private UserSummaryResponse ideaCreator;
    private List<UserSummaryResponse> designers;
    private List<LookupResponse> productTypes;
    private List<LookupResponse> niches;
    private LookupResponse occasion;
    private boolean custom;
    private String designStatus;
    private LocalDateTime approvalDate;
    private LocalDateTime createdAt;

    public static DesignSummaryResponse from(DesignDetail dd) {
        String mainMockupUrl = dd.getMockups().stream()
                .filter(DesignMockup::isMainMockup)
                .map(DesignMockup::getUrl)
                .findFirst()
                .orElse(null);

        var builder = DesignSummaryResponse.builder()
                .id(dd.getId())
                .name(dd.getName())
                .mainMockupUrl(mainMockupUrl)
                .ideaCreator(dd.getIdeaCreator() != null ? UserSummaryResponse.from(dd.getIdeaCreator()) : null)
                .designers(dd.getDesigners().stream().map(UserSummaryResponse::from).toList())
                .productTypes(dd.getProductTypes().stream().map(LookupResponse::from).toList())
                .niches(dd.getNiches().stream().map(LookupResponse::from).toList())
                .occasion(dd.getOccasion() != null ? LookupResponse.from(dd.getOccasion()) : null)
                .custom(dd.isCustom())
                .designStatus(dd.getDesignStatus() != null ? dd.getDesignStatus().name() : null)
                .approvalDate(dd.getApprovalDate())
                .createdAt(dd.getCreatedAt());

        if (dd.getCard() != null) {
            builder.cardId(dd.getCard().getId())
                    .cardName(dd.getCard().getName())
                    .boardId(dd.getCard().getColumn().getBoard().getId())
                    .boardName(dd.getCard().getColumn().getBoard().getName())
                    .boardActive(dd.getCard().getColumn().getBoard().isActive())
                    .stage(dd.getCard().getStage())
                    .status(dd.getCard().getStatus() != null ? dd.getCard().getStatus().name() : null);
        }

        return builder.build();
    }
}
