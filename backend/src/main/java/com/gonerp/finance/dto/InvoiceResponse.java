package com.gonerp.finance.dto;

import com.gonerp.finance.model.FinanceInvoice;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class InvoiceResponse {
    private Long id;
    private Long monthlyReportId;
    private Long transactionId;
    private String originalFilename;
    private String storageUrl;
    private String invoiceType;
    private String description;
    private LocalDateTime createdAt;
    private String createdBy;

    public static InvoiceResponse from(FinanceInvoice entity) {
        return InvoiceResponse.builder()
                .id(entity.getId())
                .monthlyReportId(entity.getMonthlyReport().getId())
                .transactionId(entity.getTransaction() != null ? entity.getTransaction().getId() : null)
                .originalFilename(entity.getOriginalFilename())
                .storageUrl(entity.getStorageUrl())
                .invoiceType(entity.getInvoiceType().name())
                .description(entity.getDescription())
                .createdAt(entity.getCreatedAt())
                .createdBy(entity.getCreatedBy())
                .build();
    }
}
