package com.gonerp.ecommerce.dto;

import com.gonerp.ecommerce.model.EcomStore;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class EcomStoreResponse {
    private Long id;
    private String name;
    private String salesChannel;
    private String currency;
    private String storeUrl;
    private boolean active;
    private LocalDateTime createdAt;
    private String createdBy;

    public static EcomStoreResponse from(EcomStore entity) {
        return EcomStoreResponse.builder()
                .id(entity.getId())
                .name(entity.getName())
                .salesChannel(entity.getSalesChannel() != null ? entity.getSalesChannel().name() : null)
                .currency(entity.getCurrency())
                .storeUrl(entity.getStoreUrl())
                .active(entity.isActive())
                .createdAt(entity.getCreatedAt())
                .createdBy(entity.getCreatedBy())
                .build();
    }
}
