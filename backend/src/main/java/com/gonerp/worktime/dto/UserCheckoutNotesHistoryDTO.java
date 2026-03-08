package com.gonerp.worktime.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class UserCheckoutNotesHistoryDTO {
    private Long userId;
    private String userName;
    private String firstName;
    private String lastName;
    private List<MemberCheckoutNoteDTO> entries;
}
