package com.gonerp.finance.dto;

import com.gonerp.finance.model.FinanceTransaction;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

@Data
@Builder
public class TransactionResponse {
    private Long id;
    private Long accountId;
    private Long transactionFileId;
    private Long monthlyReportId;
    private Map<String, String> rawData;
    private int rowIndex;
    private LocalDate transactionDate;
    private String counterparty;
    private String description;
    private BigDecimal amount;
    private String currency;
    private BigDecimal balanceAfter;
    private String category;
    private String subcategory;
    private String note;
    private boolean autoCategorized;
    private boolean manuallyReviewed;
    private boolean interAccount;
    private boolean completed;

    public static TransactionResponse from(FinanceTransaction entity) {
        return TransactionResponse.builder()
                .id(entity.getId())
                .accountId(entity.getAccount().getId())
                .transactionFileId(entity.getTransactionFile().getId())
                .monthlyReportId(entity.getMonthlyReport().getId())
                .rawData(entity.getRawData())
                .rowIndex(entity.getRowIndex())
                .transactionDate(entity.getTransactionDate())
                .counterparty(entity.getCounterparty())
                .description(entity.getDescription())
                .amount(entity.getAmount())
                .currency(entity.getCurrency())
                .balanceAfter(entity.getBalanceAfter())
                .category(entity.getCategory())
                .subcategory(entity.getSubcategory())
                .note(entity.getNote())
                .autoCategorized(entity.isAutoCategorized())
                .manuallyReviewed(entity.isManuallyReviewed())
                .interAccount(entity.isInterAccount())
                .completed(entity.isCompleted())
                .build();
    }
}
