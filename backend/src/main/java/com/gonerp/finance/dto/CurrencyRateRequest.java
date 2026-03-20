package com.gonerp.finance.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class CurrencyRateRequest {
    @NotBlank
    private String fromCurrency;
    @NotBlank
    private String toCurrency;
    @NotNull
    private BigDecimal rate;
    @NotNull
    private LocalDate effectiveDate;
}
