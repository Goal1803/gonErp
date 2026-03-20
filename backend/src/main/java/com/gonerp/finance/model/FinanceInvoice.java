package com.gonerp.finance.model;

import com.gonerp.common.BaseModel;
import com.gonerp.finance.model.enums.InvoiceType;
import com.gonerp.organization.model.Organization;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "fin_invoices")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FinanceInvoice extends BaseModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "monthly_report_id", nullable = false)
    private FinanceMonthlyReport monthlyReport;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_id")
    private FinanceTransaction transaction;

    @Column(name = "original_filename", nullable = false)
    private String originalFilename;

    @Column(name = "storage_url", nullable = false)
    private String storageUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "invoice_type", nullable = false, length = 30)
    @Builder.Default
    private InvoiceType invoiceType = InvoiceType.INVOICE;

    @Column(columnDefinition = "TEXT")
    private String description;
}
