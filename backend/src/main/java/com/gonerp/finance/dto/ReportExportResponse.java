package com.gonerp.finance.dto;

import com.gonerp.finance.model.FinanceReportExport;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ReportExportResponse {
    private Long id;
    private Long monthlyReportId;
    private String storageUrl;
    private String filename;
    private Long fileSize;
    private String status;
    private String errorMessage;
    private String generatedBy;
    private LocalDateTime createdAt;

    public static ReportExportResponse from(FinanceReportExport entity) {
        return ReportExportResponse.builder()
                .id(entity.getId())
                .monthlyReportId(entity.getMonthlyReport().getId())
                .storageUrl(entity.getStorageUrl())
                .filename(entity.getFilename())
                .fileSize(entity.getFileSize())
                .status(entity.getStatus().name())
                .errorMessage(entity.getErrorMessage())
                .generatedBy(entity.getGeneratedBy())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
