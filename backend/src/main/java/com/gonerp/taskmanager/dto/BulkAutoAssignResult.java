package com.gonerp.taskmanager.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BulkAutoAssignResult {
    private int processed;
    private int skipped;
    @Builder.Default
    private List<SkippedItem> skippedDetails = new ArrayList<>();

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SkippedItem {
        private Long cardId;
        private String cardName;
        private String reason;
    }
}
