package com.gonerp.worktime.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@Data
@Builder
public class MemberCheckoutNoteDTO {
    private Long userId;
    private String userName;
    private String firstName;
    private String lastName;
    private String avatarUrl;
    private String dailyNotes;
    private LocalDate workDate;
    private OffsetDateTime checkOutTime;
    private int totalWorkMinutes;
}
