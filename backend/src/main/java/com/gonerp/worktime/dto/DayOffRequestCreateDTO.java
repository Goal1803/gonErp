package com.gonerp.worktime.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class DayOffRequestCreateDTO {

    @NotNull(message = "Day-off type is required")
    private Long dayOffTypeId;

    @NotNull(message = "Start date is required")
    private LocalDate startDate;

    @NotNull(message = "End date is required")
    private LocalDate endDate;

    private String halfDayType;

    private String reason;
}
