package com.gonerp.ecommerce.model;

import com.gonerp.common.BaseModel;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "ecom_etsy_transactions", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"store_id", "txn_date", "type", "title", "info", "amount", "fees_and_taxes", "currency"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EcomEtsyTransaction extends BaseModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false)
    private EcomStore store;

    @Column(name = "txn_date")
    private LocalDate txnDate;

    @Column(nullable = false, length = 50)
    private String type;

    @Column(nullable = false, length = 1000)
    private String title;

    @Column(length = 1000)
    private String info;

    @Column(length = 10)
    private String currency;

    @Column(precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "fees_and_taxes", precision = 12, scale = 2)
    private BigDecimal feesAndTaxes;

    @Column(precision = 12, scale = 2)
    private BigDecimal net;

    @Column(name = "tax_details", length = 500)
    private String taxDetails;

    @Column(length = 50)
    private String status;

    @Column(name = "availability_date", length = 200)
    private String availabilityDate;

    // Extracted from Info field (e.g. "Order #4014148466")
    @Column(name = "order_id_ref", length = 50)
    private String orderIdRef;

    // Whether this transaction has been matched to an order's platform fee
    @Column(name = "matched", nullable = false)
    @Builder.Default
    private boolean matched = false;
}
