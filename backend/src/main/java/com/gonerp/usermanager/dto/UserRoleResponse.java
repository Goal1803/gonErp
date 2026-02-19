package com.gonerp.usermanager.dto;

import com.gonerp.usermanager.model.UserRole;
import com.gonerp.usermanager.model.enums.RoleName;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class UserRoleResponse {
    private Long id;
    private RoleName name;
    private String description;
    private LocalDateTime createdAt;
    private String createdBy;
    private LocalDateTime lastUpdatedAt;
    private String lastUpdatedBy;

    public static UserRoleResponse from(UserRole role) {
        return UserRoleResponse.builder()
                .id(role.getId())
                .name(role.getName())
                .description(role.getDescription())
                .createdAt(role.getCreatedAt())
                .createdBy(role.getCreatedBy())
                .lastUpdatedAt(role.getLastUpdatedAt())
                .lastUpdatedBy(role.getLastUpdatedBy())
                .build();
    }
}
