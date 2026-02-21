package com.gonerp.organization.dto;

import com.gonerp.organization.model.OrganizationType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class OrganizationTypeResponse {
    private Long id;
    private String name;
    private String description;
    private long staffRoleCount;
    private long departmentCount;
    private long userGroupCount;
    private LocalDateTime createdAt;
    private String createdBy;

    public static OrganizationTypeResponse from(OrganizationType orgType,
                                                 long staffRoleCount,
                                                 long departmentCount,
                                                 long userGroupCount) {
        return OrganizationTypeResponse.builder()
                .id(orgType.getId())
                .name(orgType.getName())
                .description(orgType.getDescription())
                .staffRoleCount(staffRoleCount)
                .departmentCount(departmentCount)
                .userGroupCount(userGroupCount)
                .createdAt(orgType.getCreatedAt())
                .createdBy(orgType.getCreatedBy())
                .build();
    }
}
