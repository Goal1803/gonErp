package com.gonerp.worktime.dto;

import com.gonerp.worktime.model.UserWorkTimeConfig;
import lombok.Builder;
import lombok.Data;

import java.time.LocalTime;

@Data
@Builder
public class UserWorkTimeConfigResponse {
    private Long id;
    private Long userId;
    private String userName;
    private String firstName;
    private String lastName;
    private String timezoneId;
    private LocalTime workStartTime;
    private LocalTime workEndTime;
    private double dailyWorkingHours;
    private double weeklyWorkingHours;

    public static UserWorkTimeConfigResponse from(UserWorkTimeConfig config) {
        return UserWorkTimeConfigResponse.builder()
                .id(config.getId())
                .userId(config.getUser().getId())
                .userName(config.getUser().getUserName())
                .firstName(config.getUser().getFirstName())
                .lastName(config.getUser().getLastName())
                .timezoneId(config.getTimezoneId())
                .workStartTime(config.getWorkStartTime())
                .workEndTime(config.getWorkEndTime())
                .dailyWorkingHours(config.getDailyWorkingHours())
                .weeklyWorkingHours(config.getWeeklyWorkingHours())
                .build();
    }
}
