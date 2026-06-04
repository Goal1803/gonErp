package com.gonerp.taskmanager.dto;

import com.gonerp.taskmanager.model.Card;
import com.gonerp.taskmanager.model.enums.CardGenType;
import com.gonerp.taskmanager.model.enums.CardStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

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
    private String sku;
    private CardGenType genType;
    private Long columnId;
    private Long boardId;
    private List<LabelResponse> labels;
    private List<TypeResponse> types;
    private List<UserSummaryResponse> members;
    private List<AttachmentResponse> attachments;
    private List<LinkResponse> links;
    private List<CommentResponse> comments;
    private List<ActivityResponse> activities;
    private LocalDateTime createdAt;
    private String createdBy;
    private LocalDateTime lastUpdatedAt;
    private String lastUpdatedBy;
    private DesignDetailResponse designDetail;
    private UserSummaryResponse designer;
    private LinkedOrderInfo linkedOrder;
    private boolean archived;
    private LocalDateTime archivedAt;

    @Data
    @Builder
    public static class LinkedOrderInfo {
        private Long orderId;
        private String platformOrderId;
        private String salesChannel;
        private String storeName;
        private LocalDateTime orderDate;
        private String orderStatus;
        private int numberOfItems;
        private String sku;
        private BigDecimal orderTotal;
        private String currency;
        // Customer info — null when viewer is DESIGNER
        private String customerName;
        private String buyerUserId;
        private String customerEmail;
        private String customerPhone;
        private String shipStreet1;
        private String shipStreet2;
        private String shipCity;
        private String shipState;
        private String shipZipcode;
        private String shipCountry;
        private Long supplierId;
        private String supplierName;
        private String supplierTransactionId;
        private String shippingAgent;
        private String trackingNumber;
    }

    public static CardDetailResponse from(Card card) {
        return from(card, null);
    }

    public static CardDetailResponse from(Card card, Long currentUserId) {
        var topLevelComments = card.getComments().stream()
                .filter(c -> c.getParent() == null)
                .map(c -> CommentResponse.from(c, currentUserId))
                .toList();

        return CardDetailResponse.builder()
                .id(card.getId())
                .name(card.getName())
                .description(card.getDescription())
                .status(card.getStatus())
                .stage(card.getStage())
                .position(card.getPosition())
                .mainImageUrl(card.getMainImageUrl())
                .sku(card.getSku())
                .genType(card.getGenType())
                .columnId(card.getColumn().getId())
                .boardId(card.getColumn().getBoard().getId())
                .labels(card.getLabels().stream().map(LabelResponse::from).toList())
                .types(card.getTypes().stream().map(TypeResponse::from).toList())
                .members(card.getMembers().stream()
                        .map(m -> UserSummaryResponse.from(m.getUser())).toList())
                .attachments(card.getAttachments().stream().map(AttachmentResponse::from).toList())
                .links(card.getLinks().stream().map(LinkResponse::from).toList())
                .comments(topLevelComments)
                .activities(card.getActivities().stream().map(ActivityResponse::from).toList())
                .createdAt(card.getCreatedAt())
                .createdBy(card.getCreatedBy())
                .lastUpdatedAt(card.getLastUpdatedAt())
                .lastUpdatedBy(card.getLastUpdatedBy())
                .designer(card.getDesigner() != null ? UserSummaryResponse.from(card.getDesigner()) : null)
                .archived(card.isArchived())
                .archivedAt(card.getArchivedAt())
                .build();
    }
}
