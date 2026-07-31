package com.gonerp.notes.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * Rows as one view wants them. Ungrouped views fill {@code rows}; a BOARD view
 * (or any view with groupBy set) fills {@code groups} instead.
 */
@Data
public class RowsResponse {

    private Long viewId;
    private List<RowResponse> rows = new ArrayList<>();
    private List<RowGroupResponse> groups = new ArrayList<>();
    private boolean grouped;
    private int total;
}
