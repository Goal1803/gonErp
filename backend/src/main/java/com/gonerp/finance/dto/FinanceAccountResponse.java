package com.gonerp.finance.dto;

import com.gonerp.finance.model.FinanceAccount;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
public class FinanceAccountResponse {
    private Long id;
    private String name;
    private String accountType;
    private String iban;
    private String currency;
    private String marketplace;
    private String csvDelimiter;
    private String csvEncoding;
    private int csvSkipRows;
    private Map<String, Object> columnMapping;
    private boolean active;
    private int displayOrder;
    private LocalDateTime createdAt;
    private String createdBy;

    public static FinanceAccountResponse from(FinanceAccount entity) {
        return FinanceAccountResponse.builder()
                .id(entity.getId())
                .name(entity.getName())
                .accountType(entity.getAccountType().name())
                .iban(entity.getIban())
                .currency(entity.getCurrency())
                .marketplace(entity.getMarketplace())
                .csvDelimiter(entity.getCsvDelimiter())
                .csvEncoding(entity.getCsvEncoding())
                .csvSkipRows(entity.getCsvSkipRows())
                .columnMapping(entity.getColumnMapping())
                .active(entity.isActive())
                .displayOrder(entity.getDisplayOrder())
                .createdAt(entity.getCreatedAt())
                .createdBy(entity.getCreatedBy())
                .build();
    }
}
