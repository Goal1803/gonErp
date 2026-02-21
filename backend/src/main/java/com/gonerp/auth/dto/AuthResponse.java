package com.gonerp.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {
    private String token;
    private String tokenType;
    private String userName;
    private String firstName;
    private String lastName;
    private String avatarUrl;
    private String role;
    private Long userId;
    private boolean designsManager;
    private Long organizationId;
    private String organizationName;
    private String organizationSlug;
    private boolean moduleTaskManager;
    private boolean moduleImageManager;
    private boolean moduleDesigns;
}
