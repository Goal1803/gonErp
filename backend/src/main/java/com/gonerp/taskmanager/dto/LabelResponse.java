package com.gonerp.taskmanager.dto;

import com.gonerp.taskmanager.model.CardLabel;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LabelResponse {
    private Long id;
    private String name;
    private String color;

    public static LabelResponse from(CardLabel label) {
        return LabelResponse.builder()
                .id(label.getId())
                .name(label.getName())
                .color(label.getColor())
                .build();
    }
}
