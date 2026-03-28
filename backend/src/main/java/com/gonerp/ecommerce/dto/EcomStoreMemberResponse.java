package com.gonerp.ecommerce.dto;

import com.gonerp.ecommerce.model.EcomStoreMember;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class EcomStoreMemberResponse {
    private Long id;
    private Long userId;
    private String userName;
    private String firstName;
    private String lastName;
    private String avatarUrl;
    private String storeRole;
    private LocalDateTime createdAt;
    private String createdBy;

    public static EcomStoreMemberResponse from(EcomStoreMember entity) {
        return EcomStoreMemberResponse.builder()
                .id(entity.getId())
                .userId(entity.getUser().getId())
                .userName(entity.getUser().getUserName())
                .firstName(entity.getUser().getFirstName())
                .lastName(entity.getUser().getLastName())
                .avatarUrl(entity.getUser().getAvatarUrl())
                .storeRole(entity.getStoreRole().name())
                .createdAt(entity.getCreatedAt())
                .createdBy(entity.getCreatedBy())
                .build();
    }
}
