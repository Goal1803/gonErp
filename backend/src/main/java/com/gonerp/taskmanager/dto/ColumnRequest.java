package com.gonerp.taskmanager.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ColumnRequest {
    @NotBlank(message = "Column title is required")
    private String title;
}
