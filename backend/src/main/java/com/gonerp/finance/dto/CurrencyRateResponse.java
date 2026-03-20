package com.gonerp.finance.dto;

import com.gonerp.finance.model.FinanceCurrencyRate;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
public class CurrencyRateResponse {
    private Long id;
    private String fromCurrency;
    private String toCurrency;
    private BigDecimal rate;
    private LocalDate effectiveDate;

    public static CurrencyRateResponse from(FinanceCurrencyRate entity) {
        return CurrencyRateResponse.builder()
                .id(entity.getId())
                .fromCurrency(entity.getFromCurrency())
                .toCurrency(entity.getToCurrency())
                .rate(entity.getRate())
                .effectiveDate(entity.getEffectiveDate())
                .build();
    }
}
