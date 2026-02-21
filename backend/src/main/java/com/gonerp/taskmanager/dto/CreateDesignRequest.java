package com.gonerp.taskmanager.dto;

import lombok.Data;

import java.util.List;

@Data
public class CreateDesignRequest {
    private String name;
    private Long ideaCreatorId;
    private List<Long> productTypeIds;
    private List<Long> nicheIds;
    private Long occasionId;
    private Boolean custom;
}
