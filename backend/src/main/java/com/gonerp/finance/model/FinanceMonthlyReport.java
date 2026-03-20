package com.gonerp.finance.model;

import com.gonerp.common.BaseModel;
import com.gonerp.finance.model.enums.ReportStatus;
import com.gonerp.organization.model.Organization;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "fin_monthly_reports", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"organization_id", "year", "month"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FinanceMonthlyReport extends BaseModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @Column(nullable = false)
    private int year;

    @Column(nullable = false)
    private int month;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private ReportStatus status = ReportStatus.DRAFT;

    @Column(columnDefinition = "TEXT")
    private String notes;
}
