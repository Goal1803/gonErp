package com.gonerp.finance.model;

import com.gonerp.common.BaseModel;
import com.gonerp.organization.model.Organization;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.util.Map;

@Entity
@Table(name = "fin_amazon_reconciliations", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"monthly_report_id", "marketplace"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FinanceAmazonReconciliation extends BaseModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "monthly_report_id", nullable = false)
    private FinanceMonthlyReport monthlyReport;

    @Column(nullable = false, length = 50)
    private String marketplace;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "reconciliation_data", columnDefinition = "jsonb")
    private Map<String, Object> reconciliationData;

    @Column(name = "total_sales", precision = 15, scale = 2)
    private BigDecimal totalSales;

    @Column(name = "total_fees", precision = 15, scale = 2)
    private BigDecimal totalFees;

    @Column(name = "total_payouts", precision = 15, scale = 2)
    private BigDecimal totalPayouts;

    @Column(precision = 15, scale = 2)
    private BigDecimal discrepancy;
}
