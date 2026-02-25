package com.gonerp.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDate;

@Data
public class ProfileUpdateRequest {
    @NotBlank(message = "First name is required")
    private String firstName;

    private String lastName;
    private LocalDate dateOfBirth;
}
