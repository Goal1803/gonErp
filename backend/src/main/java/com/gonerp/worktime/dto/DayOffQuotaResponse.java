package com.gonerp.worktime.dto;

import com.gonerp.worktime.model.DayOffQuota;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DayOffQuotaResponse {
    private Long id;
    private Long userId;
    private String userName;
    private String userFirstName;
    private String userLastName;
    private Long dayOffTypeId;
    private String dayOffTypeName;
    private String dayOffTypeColor;
    private int year;
    private double totalDays;
    private double usedDays;
    private double carriedOverDays;
    private double remainingDays;

    public static DayOffQuotaResponse from(DayOffQuota q) {
        return DayOffQuotaResponse.builder()
                .id(q.getId())
                .userId(q.getUser() != null ? q.getUser().getId() : null)
                .userName(q.getUser() != null ? q.getUser().getUserName() : null)
                .userFirstName(q.getUser() != null ? q.getUser().getFirstName() : null)
                .userLastName(q.getUser() != null ? q.getUser().getLastName() : null)
                .dayOffTypeId(q.getDayOffType() != null ? q.getDayOffType().getId() : null)
                .dayOffTypeName(q.getDayOffType() != null ? q.getDayOffType().getName() : null)
                .dayOffTypeColor(q.getDayOffType() != null ? q.getDayOffType().getColor() : null)
                .year(q.getYear())
                .totalDays(q.getTotalDays())
                .usedDays(q.getUsedDays())
                .carriedOverDays(q.getCarriedOverDays())
                .remainingDays(q.getTotalDays() + q.getCarriedOverDays() - q.getUsedDays())
                .build();
    }
}
