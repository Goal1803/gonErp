package com.gonerp.finance.dto;

import com.gonerp.finance.model.FinanceCategorizationRule;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
public class CategorizationRuleResponse {
    private Long id;
    private String name;
    private int priority;
    private boolean active;
    private String accountType;
    private String fieldName;
    private String matchType;
    private String matchValue;
    private Map<String, Object> additionalConditions;
    private String category;
    private String subcategory;
    private String noteTemplate;
    private LocalDateTime createdAt;
    private String createdBy;

    public static CategorizationRuleResponse from(FinanceCategorizationRule entity) {
        return CategorizationRuleResponse.builder()
                .id(entity.getId())
                .name(entity.getName())
                .priority(entity.getPriority())
                .active(entity.isActive())
                .accountType(entity.getAccountType() != null ? entity.getAccountType().name() : null)
                .fieldName(entity.getFieldName())
                .matchType(entity.getMatchType().name())
                .matchValue(entity.getMatchValue())
                .additionalConditions(entity.getAdditionalConditions())
                .category(entity.getCategory())
                .subcategory(entity.getSubcategory())
                .noteTemplate(entity.getNoteTemplate())
                .createdAt(entity.getCreatedAt())
                .createdBy(entity.getCreatedBy())
                .build();
    }
}
