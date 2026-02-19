package com.gonerp.usermanager.dto;

import com.gonerp.usermanager.model.enums.UserStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class UserRequest {

    @NotBlank(message = "Username is required")
    private String userName;

    @NotBlank(message = "First name is required")
    private String firstName;

    private String lastName;

    private LocalDate dateOfBirth;

    private UserStatus status;

    private String password;

    @NotNull(message = "Role ID is required")
    private Long roleId;
}
