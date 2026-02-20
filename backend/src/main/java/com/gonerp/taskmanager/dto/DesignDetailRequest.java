package com.gonerp.taskmanager.dto;

import lombok.Data;

import java.util.List;

@Data
public class DesignDetailRequest {
    private Long sellerId;
    private List<Long> productTypeIds;
    private List<Long> nicheIds;
    private Long occasionId;
    private Boolean custom;
}
