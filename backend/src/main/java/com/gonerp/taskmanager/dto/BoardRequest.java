package com.gonerp.taskmanager.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

@Data
public class BoardRequest {
    @NotBlank(message = "Board name is required")
    private String name;
    private String description;
    private String coverColor;
    private String boardType;
    private Integer autoArchiveDays;
    private List<Long> archiveColumnIds;
}
