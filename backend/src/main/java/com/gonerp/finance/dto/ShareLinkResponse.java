package com.gonerp.finance.dto;

import com.gonerp.finance.model.FinanceShareLink;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ShareLinkResponse {
    private Long id;
    private Long monthlyReportId;
    private int reportYear;
    private int reportMonth;
    private String token;
    private String recipientName;
    private String recipientEmail;
    private boolean active;
    private LocalDateTime expiresAt;
    private LocalDateTime lastAccessedAt;
    private int accessCount;
    private LocalDateTime createdAt;
    private String createdBy;

    public static ShareLinkResponse from(FinanceShareLink entity) {
        return ShareLinkResponse.builder()
                .id(entity.getId())
                .monthlyReportId(entity.getMonthlyReport().getId())
                .reportYear(entity.getMonthlyReport().getYear())
                .reportMonth(entity.getMonthlyReport().getMonth())
                .token(entity.getToken())
                .recipientName(entity.getRecipientName())
                .recipientEmail(entity.getRecipientEmail())
                .active(entity.isActive())
                .expiresAt(entity.getExpiresAt())
                .lastAccessedAt(entity.getLastAccessedAt())
                .accessCount(entity.getAccessCount())
                .createdAt(entity.getCreatedAt())
                .createdBy(entity.getCreatedBy())
                .build();
    }
}
