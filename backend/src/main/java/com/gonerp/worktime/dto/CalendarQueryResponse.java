package com.gonerp.worktime.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
public class CalendarQueryResponse {
    private List<CalendarUserDTO> users;
    private List<CalendarDayDTO> days;
    private List<PublicHolidayResponse> holidays;
    private LocalDate startDate;
    private LocalDate endDate;
}
