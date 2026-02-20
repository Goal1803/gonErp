package com.gonerp.usermanager.dto;

import com.gonerp.usermanager.model.User;
import com.gonerp.usermanager.model.enums.UserStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

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
                .createdAt(user.getCreatedAt())
                .createdBy(user.getCreatedBy())
                .lastUpdatedAt(user.getLastUpdatedAt())
                .lastUpdatedBy(user.getLastUpdatedBy())
                .build();
    }
}
