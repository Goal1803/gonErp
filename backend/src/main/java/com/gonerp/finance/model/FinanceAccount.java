package com.gonerp.finance.model;

import com.gonerp.common.BaseModel;
import com.gonerp.finance.model.enums.AccountType;
import com.gonerp.organization.model.Organization;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.Map;

@Entity
@Table(name = "fin_accounts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FinanceAccount extends BaseModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @Column(nullable = false, length = 200)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_type", nullable = false, length = 50)
    private AccountType accountType;

    @Column(length = 50)
    private String iban;

    @Column(nullable = false, length = 10)
    @Builder.Default
    private String currency = "EUR";

    @Column(length = 100)
    private String marketplace;

    @Column(name = "csv_delimiter", length = 5)
    @Builder.Default
    private String csvDelimiter = ";";

    @Column(name = "csv_encoding", length = 30)
    @Builder.Default
    private String csvEncoding = "UTF-8";

    @Column(name = "csv_skip_rows")
    @Builder.Default
    private int csvSkipRows = 0;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "column_mapping", columnDefinition = "jsonb")
    private Map<String, Object> columnMapping;

    @Column(nullable = false, columnDefinition = "boolean default true")
    @Builder.Default
    private boolean active = true;

    @Column(name = "display_order")
    @Builder.Default
    private int displayOrder = 0;
}
