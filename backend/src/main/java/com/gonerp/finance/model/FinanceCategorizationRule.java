package com.gonerp.finance.model;

import com.gonerp.common.BaseModel;
import com.gonerp.finance.model.enums.AccountType;
import com.gonerp.finance.model.enums.MatchType;
import com.gonerp.organization.model.Organization;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.Map;

@Entity
@Table(name = "fin_categorization_rules")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FinanceCategorizationRule extends BaseModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(nullable = false)
    @Builder.Default
    private int priority = 100;

    @Column(nullable = false, columnDefinition = "boolean default true")
    @Builder.Default
    private boolean active = true;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_type", length = 50)
    private AccountType accountType;

    @Column(name = "field_name", nullable = false, length = 50)
    private String fieldName;

    @Enumerated(EnumType.STRING)
    @Column(name = "match_type", nullable = false, length = 20)
    private MatchType matchType;

    @Column(name = "match_value", nullable = false)
    private String matchValue;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "additional_conditions", columnDefinition = "jsonb")
    private Map<String, Object> additionalConditions;

    @Column(nullable = false, length = 100)
    private String category;

    @Column(length = 100)
    private String subcategory;

    @Column(name = "note_template", columnDefinition = "TEXT")
    private String noteTemplate;
}
