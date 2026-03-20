package com.gonerp.finance.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class MonthlyReportRequest {

    @NotNull(message = "Year is required")
    @Min(2020)
    @Max(2099)
    private Integer year;

    @NotNull(message = "Month is required")
    @Min(1)
    @Max(12)
    private Integer month;

    private String notes;
    private String status;
}
