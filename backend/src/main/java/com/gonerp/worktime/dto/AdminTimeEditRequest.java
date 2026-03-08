package com.gonerp.worktime.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminTimeEditRequest {
    private OffsetDateTime checkInTime;
    private OffsetDateTime checkOutTime;
    private String dailyNotes;
    private List<BreakEditDTO> breaks;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BreakEditDTO {
        private Long id;         // null for new breaks
        private OffsetDateTime startTime;
        private OffsetDateTime endTime;
        private boolean deleted; // true to remove this break
    }
}
