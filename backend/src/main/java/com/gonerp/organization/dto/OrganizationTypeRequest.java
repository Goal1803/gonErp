package com.gonerp.organization.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class OrganizationTypeRequest {

    @NotBlank(message = "Name is required")
    private String name;

    private String description;
}
