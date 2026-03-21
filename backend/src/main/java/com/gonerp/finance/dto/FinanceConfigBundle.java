package com.gonerp.finance.dto;

import lombok.*;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FinanceConfigBundle {
    private String exportedAt;
    private String exportedBy;
    private String organizationName;
    private List<CurrencyEntry> currencies;
    private List<AccountEntry> accounts;
    private List<RuleEntry> rules;

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class CurrencyEntry {
        private String code;
        private String name;
        private String symbol;
        private boolean base;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class AccountEntry {
        private String name;
        private String accountType;
        private String iban;
        private String currency;
        private String marketplace;
        private String csvDelimiter;
        private String csvEncoding;
        private int csvSkipRows;
        private int displayOrder;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class RuleEntry {
        private String name;
        private int priority;
        private boolean active;
        private String accountType;
        private String fieldName;
        private String matchType;
        private String matchValue;
        private String category;
        private String subcategory;
        private String noteTemplate;
        private Map<String, Object> additionalConditions;
    }
}
