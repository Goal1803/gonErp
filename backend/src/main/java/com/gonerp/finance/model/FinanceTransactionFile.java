package com.gonerp.finance.model;

import com.gonerp.common.BaseModel;
import com.gonerp.finance.model.enums.ParseStatus;
import com.gonerp.finance.model.enums.UploadFileType;
import com.gonerp.organization.model.Organization;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.List;

@Entity
@Table(name = "fin_transaction_files")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FinanceTransactionFile extends BaseModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private FinanceAccount account;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "monthly_report_id", nullable = false)
    private FinanceMonthlyReport monthlyReport;

    @Column(name = "original_filename", nullable = false)
    private String originalFilename;

    @Column(name = "storage_url")
    private String storageUrl;

    @Column(name = "row_count")
    private int rowCount;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "column_headers", columnDefinition = "jsonb")
    private List<String> columnHeaders;

    @Enumerated(EnumType.STRING)
    @Column(name = "parse_status", nullable = false, length = 20)
    @Builder.Default
    private ParseStatus parseStatus = ParseStatus.PENDING;

    @Column(name = "parse_error", columnDefinition = "TEXT")
    private String parseError;

    @Enumerated(EnumType.STRING)
    @Column(name = "file_type", nullable = false, length = 20)
    @Builder.Default
    private UploadFileType fileType = UploadFileType.TRANSACTION;

    @Column(name = "subfolder", length = 200)
    private String subfolder;
}
