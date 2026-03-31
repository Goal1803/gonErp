package com.gonerp.ecommerce.dto;

import com.gonerp.ecommerce.model.EcomEtsyTransaction;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
public class EcomEtsyTransactionResponse {
    private Long id;
    private LocalDate txnDate;
    private String type;
    private String title;
    private String info;
    private String currency;
    private BigDecimal amount;
    private BigDecimal feesAndTaxes;
    private BigDecimal net;
    private String orderIdRef;
    private boolean matched;

    public static EcomEtsyTransactionResponse from(EcomEtsyTransaction t) {
        return EcomEtsyTransactionResponse.builder()
                .id(t.getId())
                .txnDate(t.getTxnDate())
                .type(t.getType())
                .title(t.getTitle())
                .info(t.getInfo())
                .currency(t.getCurrency())
                .amount(t.getAmount())
                .feesAndTaxes(t.getFeesAndTaxes())
                .net(t.getNet())
                .orderIdRef(t.getOrderIdRef())
                .matched(t.isMatched())
                .build();
    }
}
