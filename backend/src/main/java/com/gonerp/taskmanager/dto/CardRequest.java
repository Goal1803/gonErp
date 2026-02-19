package com.gonerp.taskmanager.dto;

import com.gonerp.taskmanager.model.enums.CardStatus;
import lombok.Data;

@Data
public class CardRequest {
    private String name;
    private String description;
    private CardStatus status;
    private String mainImageUrl;
}
