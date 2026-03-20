package com.gonerp.finance.dto;

import com.gonerp.finance.model.FinanceMonthlyReport;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class MonthlyReportResponse {
    private Long id;
    private int year;
    private int month;
    private String status;
    private String notes;
    private LocalDateTime createdAt;
    private String createdBy;
    private LocalDateTime lastUpdatedAt;
    private String lastUpdatedBy;

    public static MonthlyReportResponse from(FinanceMonthlyReport entity) {
        return MonthlyReportResponse.builder()
                .id(entity.getId())
                .year(entity.getYear())
                .month(entity.getMonth())
                .status(entity.getStatus().name())
                .notes(entity.getNotes())
                .createdAt(entity.getCreatedAt())
                .createdBy(entity.getCreatedBy())
                .lastUpdatedAt(entity.getLastUpdatedAt())
                .lastUpdatedBy(entity.getLastUpdatedBy())
                .build();
    }
}
