package com.gonerp.worktime.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DailyReportDTO {
    private LocalDate date;
    private LocalDateTime checkInTime;
    private LocalDateTime checkOutTime;
    private int totalWorkMinutes;
    private int totalBreakMinutes;
    private int overtimeMinutes;
    private String workLocation;
    private boolean isLateArrival;
    private boolean isEarlyDeparture;
}
