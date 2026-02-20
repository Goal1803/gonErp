package com.gonerp.taskmanager.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LinkRequest {
    @NotBlank(message = "URL is required")
    private String url;

    private String title;
}
