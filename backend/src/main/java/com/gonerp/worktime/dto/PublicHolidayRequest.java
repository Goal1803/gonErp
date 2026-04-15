package com.gonerp.worktime.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class PublicHolidayRequest {

    @NotNull(message = "Holiday start date is required")
    private LocalDate holidayDate;

    /** Optional end date for multi-day holidays. If null or equal to start, it's a single-day holiday. */
    private LocalDate endDate;

    @NotBlank(message = "Name is required")
    private String name;

    private String color;

    private Boolean isRecurring;
}
