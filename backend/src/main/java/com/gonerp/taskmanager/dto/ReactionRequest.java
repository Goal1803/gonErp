package com.gonerp.taskmanager.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ReactionRequest {
    @NotBlank(message = "Reaction type is required")
    private String reactionType;
}
