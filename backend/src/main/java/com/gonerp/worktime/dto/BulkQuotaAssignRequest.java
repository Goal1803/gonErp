package com.gonerp.worktime.dto;

import lombok.Data;

@Data
public class BulkQuotaAssignRequest {
    private Long dayOffTypeId;
    private int year;
    private double totalDays;
}
