package com.gonerp.ecommerce.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
public class EcomOrderImportResult {
    private int ordersCreated;
    private int ordersUpdated;
    private int orderItemsImported;
    private int skipped;
    private int totalRowsParsed;
    private List<RowOutcome> created;
    private List<RowOutcome> updated;
    private List<RowOutcome> skippedRows;
    private List<RowOutcome> errors;
    private List<RowOutcome> ignoredRows;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RowOutcome {
        private String orderId;
        private String reason;
        private String detail;
    }
}
