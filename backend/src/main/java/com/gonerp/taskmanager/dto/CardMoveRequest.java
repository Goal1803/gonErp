package com.gonerp.taskmanager.dto;

import lombok.Data;

@Data
public class CardMoveRequest {
    private Long targetColumnId;
    private int position;
}
