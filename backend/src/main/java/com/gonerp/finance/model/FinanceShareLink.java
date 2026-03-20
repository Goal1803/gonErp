package com.gonerp.finance.model;

import com.gonerp.common.BaseModel;
import com.gonerp.organization.model.Organization;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "fin_share_links")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FinanceShareLink extends BaseModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "monthly_report_id", nullable = false)
    private FinanceMonthlyReport monthlyReport;

    @Column(nullable = false, unique = true, length = 128)
    private String token;

    @Column(name = "recipient_name", length = 200)
    private String recipientName;

    @Column(name = "recipient_email", length = 200)
    private String recipientEmail;

    @Column(nullable = false, columnDefinition = "boolean default true")
    @Builder.Default
    private boolean active = true;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "last_accessed_at")
    private LocalDateTime lastAccessedAt;

    @Column(name = "access_count", nullable = false)
    @Builder.Default
    private int accessCount = 0;
}
