package com.gonerp.worktime.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class DayOffTypeRequest {

    @NotBlank(message = "Name is required")
    private String name;

    private String color;

    private Boolean isPaid;

    private Double defaultQuota;

    private Integer displayOrder;

    private Boolean active;
}
