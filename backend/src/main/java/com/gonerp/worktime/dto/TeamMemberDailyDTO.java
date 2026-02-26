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
public class TeamMemberDailyDTO {
    private Long userId;
    private String userName;
    private String firstName;
    private String lastName;
    private String avatarUrl;
    private LocalDateTime checkInTime;
    private LocalDateTime checkOutTime;
    private int totalWorkMinutes;
    private String status;
    private String workLocation;
    private boolean isLateArrival;
}
