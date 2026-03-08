package com.gonerp.worktime.dto;

import lombok.Data;

import java.time.LocalTime;

@Data
public class UserWorkTimeConfigRequest {
    private String timezoneId;
    private LocalTime workStartTime;
    private LocalTime workEndTime;
    private Double dailyWorkingHours;
    private Double weeklyWorkingHours;
}
