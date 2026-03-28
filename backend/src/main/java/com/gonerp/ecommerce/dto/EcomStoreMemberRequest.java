package com.gonerp.ecommerce.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class EcomStoreMemberRequest {

    @NotNull(message = "User ID is required")
    private Long userId;

    @NotBlank(message = "Store role is required")
    private String storeRole;
}
