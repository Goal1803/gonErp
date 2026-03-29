package com.gonerp.ecommerce.dto;

import com.gonerp.ecommerce.model.EcomSupplier;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class EcomSupplierResponse {
    private Long id;
    private String name;
    private String description;
    private List<String> countries;
    private String website;
    private String priceListUrl;
    private boolean active;

    public static EcomSupplierResponse from(EcomSupplier entity) {
        return EcomSupplierResponse.builder()
                .id(entity.getId())
                .name(entity.getName())
                .description(entity.getDescription())
                .countries(entity.getCountries())
                .website(entity.getWebsite())
                .priceListUrl(entity.getPriceListUrl())
                .active(entity.isActive())
                .build();
    }
}
