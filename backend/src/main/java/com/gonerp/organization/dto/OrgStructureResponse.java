package com.gonerp.organization.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class OrgStructureResponse {
    private Long id;
    private String name;
    private String description;
    private boolean isDefault;
}
