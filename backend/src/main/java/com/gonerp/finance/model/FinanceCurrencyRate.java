package com.gonerp.finance.model;

import com.gonerp.common.BaseModel;
import com.gonerp.organization.model.Organization;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "fin_currency_rates", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"organization_id", "from_currency", "to_currency", "effective_date"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FinanceCurrencyRate extends BaseModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @Column(name = "from_currency", nullable = false, length = 10)
    private String fromCurrency;

    @Column(name = "to_currency", nullable = false, length = 10)
    private String toCurrency;

    @Column(nullable = false, precision = 18, scale = 10)
    private BigDecimal rate;

    @Column(name = "effective_date", nullable = false)
    private LocalDate effectiveDate;
}
