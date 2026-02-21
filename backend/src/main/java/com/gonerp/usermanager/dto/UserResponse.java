package com.gonerp.usermanager.dto;

import com.gonerp.usermanager.model.User;
import com.gonerp.usermanager.model.enums.UserStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class UserResponse {
    private Long id;
    private String userName;
    private String firstName;
    private String lastName;
    private LocalDate dateOfBirth;
    private UserStatus status;
    private String avatarUrl;
    private UserRoleResponse role;
    private boolean designsManager;
    private Long organizationId;
    private String organizationName;
    private List<String> staffRoles;
    private List<String> departments;
    private List<String> userGroups;
    private LocalDateTime createdAt;
    private String createdBy;
    private LocalDateTime lastUpdatedAt;
    private String lastUpdatedBy;

    public static UserResponse from(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .userName(user.getUserName())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .dateOfBirth(user.getDateOfBirth())
                .status(user.getStatus())
                .avatarUrl(user.getAvatarUrl())
                .role(user.getRole() != null ? UserRoleResponse.from(user.getRole()) : null)
                .designsManager(user.isDesignsManager())
                .organizationId(user.getOrganization() != null ? user.getOrganization().getId() : null)
                .organizationName(user.getOrganization() != null ? user.getOrganization().getName() : null)
                .staffRoles(user.getStaffRoles() != null
                        ? user.getStaffRoles().stream().map(usr -> usr.getStaffRole().getName()).toList()
                        : List.of())
                .departments(user.getDepartments() != null
                        ? user.getDepartments().stream().map(ud -> ud.getDepartment().getName()).toList()
                        : List.of())
                .userGroups(user.getUserGroups() != null
                        ? user.getUserGroups().stream().map(uug -> uug.getUserGroup().getName()).toList()
                        : List.of())
                .createdAt(user.getCreatedAt())
                .createdBy(user.getCreatedBy())
                .lastUpdatedAt(user.getLastUpdatedAt())
                .lastUpdatedBy(user.getLastUpdatedBy())
                .build();
    }
}
