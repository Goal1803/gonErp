package com.gonerp.worktime.dto;

import lombok.Data;

import java.time.LocalTime;

@Data
public class WorkTimeSettingsRequest {
    private Double dailyWorkingHours;
    private Double weeklyFullTimeHours;
    private Double weeklyPartTimeHours;
    private Integer requiredBreakMinutes;
    private Boolean breakCountsAsWork;
    private Boolean overtimeTrackingEnabled;
    private Boolean lateEarlyTrackingEnabled;
    private LocalTime workStartTime;
    private LocalTime workEndTime;
    private Boolean carryOverEnabled;
    private Integer maxCarryOverDays;
    private Boolean dailyNotesEnabled;
    private Boolean workLocationEnabled;
    private Integer autoCheckoutReminderMinutes;
}
