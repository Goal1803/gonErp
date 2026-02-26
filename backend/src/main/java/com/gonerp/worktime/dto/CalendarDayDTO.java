package com.gonerp.worktime.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class CalendarDayDTO {
    private LocalDate date;
    private Long userId;
    private String userName;
    private String userFirstName;
    private String userLastName;
    private String userAvatarUrl;
    private Long dayOffTypeId;
    private String dayOffTypeName;
    private String dayOffTypeColor;
    private Long requestId;
    private String status;       // "APPROVED" or "PENDING"
    private String halfDayType;
    private boolean isHoliday;
    private String holidayName;
}
