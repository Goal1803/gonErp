package com.gonerp.worktime.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
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

    @JsonProperty("isClockedIn")
    private boolean isClockedIn;

    @JsonProperty("isOnBreak")
    private boolean isOnBreak;

    private Long currentEntryId;
    private LocalDateTime checkInTime;
    private String workLocation;
    private int elapsedWorkMinutes;
    private int elapsedBreakMinutes;
}
