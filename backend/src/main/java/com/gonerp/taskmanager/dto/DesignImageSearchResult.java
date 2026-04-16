package com.gonerp.taskmanager.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class DesignImageSearchResult {
    private DesignSummaryResponse design;
    /** Hamming distance (0-64). Lower is more similar; 0 = identical. */
    private int distance;
}
