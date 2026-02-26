package com.gonerp.worktime.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeamMemberMonthlyDTO {
    private Long userId;
    private String userName;
    private String firstName;
    private String lastName;
    private int totalWorkMinutes;
    private int daysWorked;
    private int overtimeMinutes;
    private int lateArrivals;
    private int earlyDepartures;
    private int daysOff;
}
