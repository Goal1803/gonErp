package com.gonerp.finance.model;

import com.gonerp.common.BaseModel;
import com.gonerp.organization.model.Organization;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

@Entity
@Table(name = "fin_transactions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FinanceTransaction extends BaseModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_file_id", nullable = false)
    private FinanceTransactionFile transactionFile;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private FinanceAccount account;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "monthly_report_id", nullable = false)
    private FinanceMonthlyReport monthlyReport;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "raw_data", columnDefinition = "jsonb", updatable = false)
    private Map<String, String> rawData;

    @Column(name = "row_index")
    private int rowIndex;

    // Canonical extracted fields
    @Column(name = "transaction_date")
    private LocalDate transactionDate;

    @Column(length = 500)
    private String counterparty;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(length = 10)
    private String currency;

    @Column(name = "balance_after", precision = 15, scale = 2)
    private BigDecimal balanceAfter;

    // Appended fields (editable)
    @Column(length = 100)
    private String category;

    @Column(length = 100)
    private String subcategory;

    @Column(columnDefinition = "TEXT")
    private String note;

    @Column(name = "auto_categorized", nullable = false, columnDefinition = "boolean default false")
    @Builder.Default
    private boolean autoCategorized = false;

    @Column(name = "manually_reviewed", nullable = false, columnDefinition = "boolean default false")
    @Builder.Default
    private boolean manuallyReviewed = false;

    @Column(name = "inter_account", nullable = false, columnDefinition = "boolean default false")
    @Builder.Default
    private boolean interAccount = false;

    @Column(name = "completed", nullable = false, columnDefinition = "boolean default false")
    @Builder.Default
    private boolean completed = false;
}
