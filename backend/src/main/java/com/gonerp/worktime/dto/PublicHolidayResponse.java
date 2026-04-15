package com.gonerp.worktime.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.gonerp.worktime.model.PublicHoliday;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class PublicHolidayResponse {
    private Long id;
    private LocalDate holidayDate;
    private LocalDate endDate;
    private String name;
    private String color;
    @JsonProperty("isRecurring")
    private boolean isRecurring;
    private Long organizationId;

    public static PublicHolidayResponse from(PublicHoliday holiday) {
        return PublicHolidayResponse.builder()
                .id(holiday.getId())
                .holidayDate(holiday.getHolidayDate())
                .endDate(holiday.getEndDate())
                .name(holiday.getName())
                .color(holiday.getColor())
                .isRecurring(holiday.isRecurring())
                .organizationId(holiday.getOrganization() != null ? holiday.getOrganization().getId() : null)
                .build();
    }
}
