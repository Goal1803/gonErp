package com.gonerp.worktime.dto;

import com.gonerp.worktime.model.DayOffRequest;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class DayOffRequestResponse {
    private Long id;
    private Long userId;
    private String userName;
    private String userFirstName;
    private String userLastName;
    private String userAvatarUrl;
    private Long dayOffTypeId;
    private String dayOffTypeName;
    private String dayOffTypeColor;
    private LocalDate startDate;
    private LocalDate endDate;
    private String halfDayType;
    private double totalDays;
    private String reason;
    private String status;
    private Long reviewedById;
    private String reviewedByName;
    private LocalDateTime reviewedAt;
    private String reviewComment;
    private LocalDateTime createdAt;

    public static DayOffRequestResponse from(DayOffRequest r) {
        return DayOffRequestResponse.builder()
                .id(r.getId())
                .userId(r.getUser() != null ? r.getUser().getId() : null)
                .userName(r.getUser() != null ? r.getUser().getUserName() : null)
                .userFirstName(r.getUser() != null ? r.getUser().getFirstName() : null)
                .userLastName(r.getUser() != null ? r.getUser().getLastName() : null)
                .userAvatarUrl(r.getUser() != null ? r.getUser().getAvatarUrl() : null)
                .dayOffTypeId(r.getDayOffType() != null ? r.getDayOffType().getId() : null)
                .dayOffTypeName(r.getDayOffType() != null ? r.getDayOffType().getName() : null)
                .dayOffTypeColor(r.getDayOffType() != null ? r.getDayOffType().getColor() : null)
                .startDate(r.getStartDate())
                .endDate(r.getEndDate())
                .halfDayType(r.getHalfDayType() != null ? r.getHalfDayType().name() : null)
                .totalDays(r.getTotalDays())
                .reason(r.getReason())
                .status(r.getStatus() != null ? r.getStatus().name() : null)
                .reviewedById(r.getReviewedBy() != null ? r.getReviewedBy().getId() : null)
                .reviewedByName(r.getReviewedBy() != null ? r.getReviewedBy().getUserName() : null)
                .reviewedAt(r.getReviewedAt())
                .reviewComment(r.getReviewComment())
                .createdAt(r.getCreatedAt())
                .build();
    }
}
