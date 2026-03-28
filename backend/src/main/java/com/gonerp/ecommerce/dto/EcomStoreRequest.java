package com.gonerp.ecommerce.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class EcomStoreRequest {

    @NotBlank(message = "Store name is required")
    private String name;

    private String salesChannel;
    private String currency;
    private String storeUrl;
    private Boolean active;
}
