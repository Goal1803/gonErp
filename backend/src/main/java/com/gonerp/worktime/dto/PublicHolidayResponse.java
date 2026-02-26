package com.gonerp.worktime.dto;

import com.gonerp.worktime.model.PublicHoliday;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class PublicHolidayResponse {
    private Long id;
    private LocalDate holidayDate;
    private String name;
    private boolean isRecurring;
    private Long organizationId;

    public static PublicHolidayResponse from(PublicHoliday holiday) {
        return PublicHolidayResponse.builder()
                .id(holiday.getId())
                .holidayDate(holiday.getHolidayDate())
                .name(holiday.getName())
                .isRecurring(holiday.isRecurring())
                .organizationId(holiday.getOrganization() != null ? holiday.getOrganization().getId() : null)
                .build();
    }
}
