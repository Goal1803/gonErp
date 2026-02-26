package com.gonerp.worktime.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WeeklyReportDTO {
    private LocalDate weekStart;
    private LocalDate weekEnd;
    private int totalWorkMinutes;
    private int totalBreakMinutes;
    private int totalOvertimeMinutes;
    private int daysWorked;
    private List<DailyReportDTO> dailyEntries;
    private int averageWorkMinutesPerDay;
}
