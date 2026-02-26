package com.gonerp.worktime.dto;

import lombok.Data;

@Data
public class DayOffQuotaUpdateRequest {
    private Double totalDays;
    private Double carriedOverDays;
}
