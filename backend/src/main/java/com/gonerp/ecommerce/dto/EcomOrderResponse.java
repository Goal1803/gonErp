package com.gonerp.ecommerce.dto;

import com.gonerp.ecommerce.model.EcomOrder;
import com.gonerp.ecommerce.model.EcomOrderItem;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class EcomOrderResponse {
    private Long id;
    private Long storeId;
    private String storeName;
    private String salesChannel;
    private String platformOrderId;
    private LocalDateTime orderDate;
    private int numberOfItems;
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
    private String currency;
    private BigDecimal orderValue;
    private BigDecimal shippingPrice;
    private BigDecimal tax;
    private BigDecimal discount;
    private BigDecimal processingFees;
    private BigDecimal orderTotal;
    private BigDecimal orderNet;
    private BigDecimal fulfillmentCost;
    private BigDecimal otherCost;
    private BigDecimal grossProfit;
    private String status;
    private String trackingNumber;
    private String trackingStatus;
    private LocalDateTime shippedDate;
    private boolean refunded;
    private String notes;
    private String sku;
    private List<OrderItemResponse> items;
    private Map<String, String> rawData;
    private Map<String, Object> platformData;
    private Long cardId;
    private LocalDateTime createdAt;
    private String createdBy;

    @Data
    @Builder
    public static class OrderItemResponse {
        private Long id;
        private String platformItemId;
        private String productName;
        private String sku;
        private String listingId;
        private String variations;
        private int quantity;
        private BigDecimal itemPrice;
        private BigDecimal itemTotal;

        public static OrderItemResponse from(EcomOrderItem item) {
            return OrderItemResponse.builder()
                    .id(item.getId())
                    .platformItemId(item.getPlatformItemId())
                    .productName(item.getProductName())
                    .sku(item.getSku())
                    .listingId(item.getListingId())
                    .variations(item.getVariations())
                    .quantity(item.getQuantity())
                    .itemPrice(item.getItemPrice())
                    .itemTotal(item.getItemTotal())
                    .build();
        }
    }

    public static EcomOrderResponse from(EcomOrder entity) {
        return EcomOrderResponse.builder()
                .id(entity.getId())
                .storeId(entity.getStore() != null ? entity.getStore().getId() : null)
                .storeName(entity.getStore() != null ? entity.getStore().getName() : null)
                .salesChannel(entity.getSalesChannel() != null ? entity.getSalesChannel().name() : null)
                .platformOrderId(entity.getPlatformOrderId())
                .orderDate(entity.getOrderDate())
                .numberOfItems(entity.getNumberOfItems())
                .customerName(entity.getCustomerName())
                .buyerUserId(entity.getBuyerUserId())
                .customerEmail(entity.getCustomerEmail())
                .customerPhone(entity.getCustomerPhone())
                .shipStreet1(entity.getShipStreet1())
                .shipStreet2(entity.getShipStreet2())
                .shipCity(entity.getShipCity())
                .shipState(entity.getShipState())
                .shipZipcode(entity.getShipZipcode())
                .shipCountry(entity.getShipCountry())
                .currency(entity.getCurrency())
                .orderValue(entity.getOrderValue())
                .shippingPrice(entity.getShippingPrice())
                .tax(entity.getTax())
                .discount(entity.getDiscount())
                .processingFees(entity.getProcessingFees())
                .orderTotal(entity.getOrderTotal())
                .orderNet(entity.getOrderNet())
                .fulfillmentCost(entity.getFulfillmentCost())
                .otherCost(entity.getOtherCost())
                .grossProfit(entity.getGrossProfit())
                .status(entity.getStatus() != null ? entity.getStatus().name() : null)
                .trackingNumber(entity.getTrackingNumber())
                .trackingStatus(entity.getTrackingStatus() != null ? entity.getTrackingStatus().name() : null)
                .shippedDate(entity.getShippedDate())
                .refunded(entity.isRefunded())
                .notes(entity.getNotes())
                .sku(entity.getSku())
                .items(entity.getItems() != null
                        ? entity.getItems().stream().map(OrderItemResponse::from).toList()
                        : Collections.emptyList())
                .rawData(entity.getRawData())
                .platformData(entity.getPlatformData())
                .cardId(entity.getCard() != null ? entity.getCard().getId() : null)
                .createdAt(entity.getCreatedAt())
                .createdBy(entity.getCreatedBy())
                .build();
    }
}
