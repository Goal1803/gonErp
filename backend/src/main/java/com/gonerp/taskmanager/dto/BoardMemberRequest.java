package com.gonerp.taskmanager.dto;

import com.gonerp.taskmanager.model.enums.BoardMemberRole;
import lombok.Data;

@Data
public class BoardMemberRequest {
    private Long userId;
    private BoardMemberRole role;
}
