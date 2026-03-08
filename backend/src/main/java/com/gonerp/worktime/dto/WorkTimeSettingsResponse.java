package com.gonerp.worktime.dto;

import com.gonerp.worktime.model.WorkTimeSettings;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkTimeSettingsResponse {
    private Long id;
    private Long organizationId;
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
    private Integer breakReminderMinutes;
    private LocalTime forceCheckoutTime;
    private String timezoneId;

    public static WorkTimeSettingsResponse from(WorkTimeSettings s) {
        return WorkTimeSettingsResponse.builder()
                .id(s.getId())
                .organizationId(s.getOrganization() != null ? s.getOrganization().getId() : null)
                .dailyWorkingHours(s.getDailyWorkingHours())
                .weeklyFullTimeHours(s.getWeeklyFullTimeHours())
                .weeklyPartTimeHours(s.getWeeklyPartTimeHours())
                .requiredBreakMinutes(s.getRequiredBreakMinutes())
                .breakCountsAsWork(s.isBreakCountsAsWork())
                .overtimeTrackingEnabled(s.isOvertimeTrackingEnabled())
                .lateEarlyTrackingEnabled(s.isLateEarlyTrackingEnabled())
                .workStartTime(s.getWorkStartTime())
                .workEndTime(s.getWorkEndTime())
                .carryOverEnabled(s.isCarryOverEnabled())
                .maxCarryOverDays(s.getMaxCarryOverDays())
                .dailyNotesEnabled(s.isDailyNotesEnabled())
                .workLocationEnabled(s.isWorkLocationEnabled())
                .autoCheckoutReminderMinutes(s.getAutoCheckoutReminderMinutes())
                .breakReminderMinutes(s.getBreakReminderMinutes())
                .forceCheckoutTime(s.getForceCheckoutTime())
                .timezoneId(s.getTimezoneId())
                .build();
    }
}
