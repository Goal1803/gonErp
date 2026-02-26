package com.gonerp.organization.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class OrganizationRequest {

    @NotBlank(message = "Name is required")
    private String name;

    @NotBlank(message = "Slug is required")
    private String slug;

    private Boolean active;

    @NotNull(message = "Organization type is required")
    private Long orgTypeId;

    private Boolean moduleTaskManager;
    private Boolean moduleImageManager;
    private Boolean moduleDesigns;
    private Boolean moduleWorkTime;

    // For creating first admin user (only used on create)
    private String adminUserName;
    private String adminPassword;
    private String adminFirstName;
    private String adminLastName;
}
