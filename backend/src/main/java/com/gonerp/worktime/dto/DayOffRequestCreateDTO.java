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

    /** Used for single-day requests (startDate == endDate). */
    private String halfDayType;

    /** Used for multi-day requests: half-day on the first day. FULL_DAY / MORNING / AFTERNOON / null. */
    private String startHalfDayType;

    /** Used for multi-day requests: half-day on the last day. */
    private String endHalfDayType;

    private String reason;
}
