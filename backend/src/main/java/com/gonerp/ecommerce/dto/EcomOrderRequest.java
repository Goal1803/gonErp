package com.gonerp.ecommerce.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class EcomOrderRequest {
    private String status;
    private String trackingNumber;
    private String trackingStatus;
    private BigDecimal fulfillmentCost;
    private BigDecimal otherCost;
    private String notes;
    private Boolean refunded;
}
