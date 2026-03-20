package com.gonerp.finance.dto;

import com.gonerp.finance.model.FinanceCurrency;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CurrencyResponse {
    private Long id;
    private String code;
    private String name;
    private String symbol;
    private boolean base;
    private boolean active;

    public static CurrencyResponse from(FinanceCurrency entity) {
        return CurrencyResponse.builder()
                .id(entity.getId())
                .code(entity.getCode())
                .name(entity.getName())
                .symbol(entity.getSymbol())
                .base(entity.isBase())
                .active(entity.isActive())
                .build();
    }
}
