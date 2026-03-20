package com.gonerp.finance.dto;

import com.gonerp.finance.model.FinanceAmazonReconciliation;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
public class AmazonReconciliationResponse {
    private Long id;
    private Long monthlyReportId;
    private String marketplace;
    private Map<String, Object> reconciliationData;
    private BigDecimal totalSales;
    private BigDecimal totalFees;
    private BigDecimal totalPayouts;
    private BigDecimal discrepancy;
    private LocalDateTime createdAt;
    private String createdBy;

    public static AmazonReconciliationResponse from(FinanceAmazonReconciliation entity) {
        return AmazonReconciliationResponse.builder()
                .id(entity.getId())
                .monthlyReportId(entity.getMonthlyReport().getId())
                .marketplace(entity.getMarketplace())
                .reconciliationData(entity.getReconciliationData())
                .totalSales(entity.getTotalSales())
                .totalFees(entity.getTotalFees())
                .totalPayouts(entity.getTotalPayouts())
                .discrepancy(entity.getDiscrepancy())
                .createdAt(entity.getCreatedAt())
                .createdBy(entity.getCreatedBy())
                .build();
    }
}
