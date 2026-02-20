package com.gonerp.taskmanager.dto;

import com.gonerp.taskmanager.model.Niche;
import com.gonerp.taskmanager.model.Occasion;
import com.gonerp.taskmanager.model.ProductType;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LookupResponse {
    private Long id;
    private String name;
    private String description;

    public static LookupResponse from(ProductType pt) {
        return LookupResponse.builder()
                .id(pt.getId())
                .name(pt.getName())
                .description(pt.getDescription())
                .build();
    }

    public static LookupResponse from(Niche n) {
        return LookupResponse.builder()
                .id(n.getId())
                .name(n.getName())
                .description(n.getDescription())
                .build();
    }

    public static LookupResponse from(Occasion o) {
        return LookupResponse.builder()
                .id(o.getId())
                .name(o.getName())
                .description(o.getDescription())
                .build();
    }
}
