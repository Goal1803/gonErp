package com.gonerp.worktime.dto;

import com.gonerp.worktime.model.DayOffType;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DayOffTypeResponse {
    private Long id;
    private String name;
    private String color;
    private boolean isPaid;
    private double defaultQuota;
    private int displayOrder;
    private boolean active;
    private Long organizationId;

    public static DayOffTypeResponse from(DayOffType type) {
        return DayOffTypeResponse.builder()
                .id(type.getId())
                .name(type.getName())
                .color(type.getColor())
                .isPaid(type.isPaid())
                .defaultQuota(type.getDefaultQuota())
                .displayOrder(type.getDisplayOrder())
                .active(type.isActive())
                .organizationId(type.getOrganization() != null ? type.getOrganization().getId() : null)
                .build();
    }
}
