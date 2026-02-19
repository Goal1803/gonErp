package com.gonerp.usermanager.dto;

import com.gonerp.usermanager.model.enums.RoleName;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UserRoleRequest {
    @NotNull(message = "Role name is required")
    private RoleName name;
    private String description;
}
