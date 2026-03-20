package com.gonerp.finance.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.Map;

@Data
public class CategorizationRuleRequest {
    @NotBlank(message = "Rule name is required")
    private String name;
    private Integer priority;
    private Boolean active;
    private String accountType;
    @NotBlank(message = "Field name is required")
    private String fieldName;
    @NotBlank(message = "Match type is required")
    private String matchType;
    @NotBlank(message = "Match value is required")
    private String matchValue;
    private Map<String, Object> additionalConditions;
    @NotBlank(message = "Category is required")
    private String category;
    private String subcategory;
    private String noteTemplate;
}
