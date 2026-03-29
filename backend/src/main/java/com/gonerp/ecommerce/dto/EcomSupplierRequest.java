package com.gonerp.ecommerce.dto;

import lombok.Data;
import java.util.List;

@Data
public class EcomSupplierRequest {
    private String name;
    private String description;
    private List<String> countries;
    private String website;
    private String priceListUrl;
    private Boolean active;
}
