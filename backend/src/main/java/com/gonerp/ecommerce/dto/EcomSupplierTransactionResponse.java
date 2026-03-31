package com.gonerp.ecommerce.dto;

import com.gonerp.ecommerce.model.EcomSupplierTransaction;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class EcomSupplierTransactionResponse {
    private Long id;
    private String supplierOrderId;
    private LocalDateTime orderDate;
    private BigDecimal amount;
    private String currency;
    private int quantity;
    private String status;
    private String fullName;
    private String country;
    private String streetAddress;
    private String city;
    private String stateRegion;
    private String postalCode;
    private String shipMethod;
    private String trackingId;
    private String sku;
    private String size;
    private boolean matched;
    private Long matchedOrderId;
    private String matchedPlatformOrderId;

    public static EcomSupplierTransactionResponse from(EcomSupplierTransaction t) {
        return EcomSupplierTransactionResponse.builder()
                .id(t.getId())
                .supplierOrderId(t.getSupplierOrderId())
                .orderDate(t.getOrderDate())
                .amount(t.getAmount())
                .currency(t.getCurrency())
                .quantity(t.getQuantity())
                .status(t.getStatus())
                .fullName(t.getFullName())
                .country(t.getCountry())
                .streetAddress(t.getStreetAddress())
                .city(t.getCity())
                .stateRegion(t.getStateRegion())
                .postalCode(t.getPostalCode())
                .shipMethod(t.getShipMethod())
                .trackingId(t.getTrackingId())
                .sku(t.getSku())
                .size(t.getSize())
                .matched(t.isMatched())
                .matchedOrderId(t.getMatchedOrder() != null ? t.getMatchedOrder().getId() : null)
                .matchedPlatformOrderId(t.getMatchedOrder() != null ? t.getMatchedOrder().getPlatformOrderId() : null)
                .build();
    }
}
