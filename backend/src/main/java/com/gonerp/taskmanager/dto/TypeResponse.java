package com.gonerp.taskmanager.dto;

import com.gonerp.taskmanager.model.CardType;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TypeResponse {
    private Long id;
    private String name;
    private String color;
    private String textColor;

    public static TypeResponse from(CardType type) {
        return TypeResponse.builder()
                .id(type.getId())
                .name(type.getName())
                .color(type.getColor())
                .textColor(type.getTextColor() != null ? type.getTextColor() : "#ffffff")
                .build();
    }
}
