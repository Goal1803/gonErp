package com.gonerp.taskmanager.dto;

import com.gonerp.taskmanager.model.BoardMember;
import com.gonerp.taskmanager.model.enums.BoardMemberRole;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class BoardMemberResponse {
    private Long id;
    private UserSummaryResponse user;
    private BoardMemberRole role;

    public static BoardMemberResponse from(BoardMember member) {
        return BoardMemberResponse.builder()
                .id(member.getId())
                .user(UserSummaryResponse.from(member.getUser()))
                .role(member.getRole())
                .build();
    }
}
