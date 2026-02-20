package com.gonerp.taskmanager.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LookupRequest {
    @NotBlank(message = "Name is required")
    private String name;
    private String description;
}
