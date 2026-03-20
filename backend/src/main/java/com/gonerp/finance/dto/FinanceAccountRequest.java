package com.gonerp.finance.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Map;

@Data
public class FinanceAccountRequest {

    @NotBlank(message = "Account name is required")
    private String name;

    @NotBlank(message = "Account type is required")
    private String accountType;

    private String iban;
    private String currency;
    private String marketplace;
    private String csvDelimiter;
    private String csvEncoding;
    private Integer csvSkipRows;
    private Map<String, Object> columnMapping;
    private Boolean active;
    private Integer displayOrder;
}
