package com.gonerp.finance.dto;

import com.gonerp.finance.model.FinanceTransactionFile;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class TransactionFileResponse {
    private Long id;
    private Long accountId;
    private String accountName;
    private Long monthlyReportId;
    private String originalFilename;
    private String storageUrl;
    private int rowCount;
    private List<String> columnHeaders;
    private String parseStatus;
    private String parseError;
    private String fileType;
    private String subfolder;
    private LocalDateTime createdAt;
    private String createdBy;

    public static TransactionFileResponse from(FinanceTransactionFile entity) {
        return TransactionFileResponse.builder()
                .id(entity.getId())
                .accountId(entity.getAccount().getId())
                .accountName(entity.getAccount().getName())
                .monthlyReportId(entity.getMonthlyReport().getId())
                .originalFilename(entity.getOriginalFilename())
                .storageUrl(entity.getStorageUrl())
                .rowCount(entity.getRowCount())
                .columnHeaders(entity.getColumnHeaders())
                .parseStatus(entity.getParseStatus().name())
                .parseError(entity.getParseError())
                .fileType(entity.getFileType().name())
                .subfolder(entity.getSubfolder())
                .createdAt(entity.getCreatedAt())
                .createdBy(entity.getCreatedBy())
                .build();
    }
}
