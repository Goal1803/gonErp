package com.gonerp.taskmanager.dto;

import lombok.Data;

import java.util.List;

@Data
public class ReorderRequest {
    private List<Long> orderedIds;
}
