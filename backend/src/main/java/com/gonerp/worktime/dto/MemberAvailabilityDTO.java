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
public class MemberAvailabilityDTO {
    private Long userId;
    private String userName;
    private String firstName;
    private String lastName;
    private String avatarUrl;
    private String status; // WORKING, ON_BREAK, OFF, DAY_OFF, NOT_CHECKED_IN
    private LocalDateTime checkInTime;
    private String workLocation;
}
