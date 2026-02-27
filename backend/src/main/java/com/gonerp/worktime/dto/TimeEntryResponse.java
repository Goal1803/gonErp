package com.gonerp.worktime.dto;

import com.gonerp.worktime.model.TimeEntry;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TimeEntryResponse {
    private Long id;
    private Long userId;
    private String userName;
    private String userFirstName;
    private String userLastName;
    private LocalDate workDate;
    private LocalDateTime checkInTime;
    private LocalDateTime checkOutTime;
    private String status;
    private String workLocation;
    private int totalWorkMinutes;
    private int totalBreakMinutes;
    private int overtimeMinutes;

    @com.fasterxml.jackson.annotation.JsonProperty("isLateArrival")
    private boolean isLateArrival;

    @com.fasterxml.jackson.annotation.JsonProperty("isEarlyDeparture")
    private boolean isEarlyDeparture;

    private String dailyNotes;
    private List<BreakEntryResponse> breaks;

    public static TimeEntryResponse from(TimeEntry e) {
        return TimeEntryResponse.builder()
                .id(e.getId())
                .userId(e.getUser() != null ? e.getUser().getId() : null)
                .userName(e.getUser() != null ? e.getUser().getUserName() : null)
                .userFirstName(e.getUser() != null ? e.getUser().getFirstName() : null)
                .userLastName(e.getUser() != null ? e.getUser().getLastName() : null)
                .workDate(e.getWorkDate())
                .checkInTime(e.getCheckInTime())
                .checkOutTime(e.getCheckOutTime())
                .status(e.getStatus() != null ? e.getStatus().name() : null)
                .workLocation(e.getWorkLocation() != null ? e.getWorkLocation().name() : null)
                .totalWorkMinutes(e.getTotalWorkMinutes())
                .totalBreakMinutes(e.getTotalBreakMinutes())
                .overtimeMinutes(e.getOvertimeMinutes())
                .isLateArrival(e.isLateArrival())
                .isEarlyDeparture(e.isEarlyDeparture())
                .dailyNotes(e.getDailyNotes())
                .breaks(e.getBreaks() != null
                        ? e.getBreaks().stream().map(BreakEntryResponse::from).toList()
                        : List.of())
                .build();
    }
}
