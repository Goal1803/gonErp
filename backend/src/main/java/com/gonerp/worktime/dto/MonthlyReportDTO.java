package com.gonerp.worktime.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MonthlyReportDTO {
    private int year;
    private int month;
    private int totalWorkMinutes;
    private int totalBreakMinutes;
    private int totalOvertimeMinutes;
    private int daysWorked;
    private int daysOff;
    private int lateArrivals;
    private int earlyDepartures;
    private List<WeeklyReportDTO> weeklyBreakdown;
}
