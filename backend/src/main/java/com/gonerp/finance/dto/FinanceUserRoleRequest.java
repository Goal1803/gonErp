package com.gonerp.finance.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class FinanceUserRoleRequest {

    @NotNull(message = "User ID is required")
    private Long userId;

    @NotBlank(message = "Finance role is required")
    private String financeRole;
}
