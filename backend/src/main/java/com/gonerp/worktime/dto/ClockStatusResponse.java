package com.gonerp.worktime.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClockStatusResponse {
    private String status; // CHECKED_IN, ON_BREAK, CHECKED_OUT, or null (no entry)
    private boolean isClockedIn;
    private boolean isOnBreak;
    private Long currentEntryId;
    private LocalDateTime checkInTime;
    private String workLocation;
    private int elapsedWorkMinutes;
    private int elapsedBreakMinutes;
}
