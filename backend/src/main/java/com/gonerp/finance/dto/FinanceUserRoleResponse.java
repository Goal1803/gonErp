package com.gonerp.finance.dto;

import com.gonerp.finance.model.FinanceUserRole;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class FinanceUserRoleResponse {
    private Long id;
    private Long userId;
    private String userName;
    private String firstName;
    private String lastName;
    private String avatarUrl;
    private String financeRole;
    private LocalDateTime createdAt;
    private String createdBy;

    public static FinanceUserRoleResponse from(FinanceUserRole entity) {
        return FinanceUserRoleResponse.builder()
                .id(entity.getId())
                .userId(entity.getUser().getId())
                .userName(entity.getUser().getUserName())
                .firstName(entity.getUser().getFirstName())
                .lastName(entity.getUser().getLastName())
                .avatarUrl(entity.getUser().getAvatarUrl())
                .financeRole(entity.getFinanceRole().name())
                .createdAt(entity.getCreatedAt())
                .createdBy(entity.getCreatedBy())
                .build();
    }
}
