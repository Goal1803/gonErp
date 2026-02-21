package com.gonerp.organization.dto;

import com.gonerp.organization.model.Organization;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class OrganizationResponse {
    private Long id;
    private String name;
    private String slug;
    private boolean active;
    private boolean moduleTaskManager;
    private boolean moduleImageManager;
    private boolean moduleDesigns;
    private Long orgTypeId;
    private String orgTypeName;
    private long userCount;
    private LocalDateTime createdAt;
    private String createdBy;

    public static OrganizationResponse from(Organization org, long userCount) {
        return OrganizationResponse.builder()
                .id(org.getId())
                .name(org.getName())
                .slug(org.getSlug())
                .active(org.isActive())
                .moduleTaskManager(org.isModuleTaskManager())
                .moduleImageManager(org.isModuleImageManager())
                .moduleDesigns(org.isModuleDesigns())
                .orgTypeId(org.getOrgType() != null ? org.getOrgType().getId() : null)
                .orgTypeName(org.getOrgType() != null ? org.getOrgType().getName() : null)
                .userCount(userCount)
                .createdAt(org.getCreatedAt())
                .createdBy(org.getCreatedBy())
                .build();
    }
}
