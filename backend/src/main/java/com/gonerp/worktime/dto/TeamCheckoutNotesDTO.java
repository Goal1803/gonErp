package com.gonerp.worktime.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
public class TeamCheckoutNotesDTO {
    private LocalDate date;
    private List<MemberCheckoutNoteDTO> entries;
}
