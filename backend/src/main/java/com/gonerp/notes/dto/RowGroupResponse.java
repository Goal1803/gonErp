package com.gonerp.notes.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/** A board column / grouped section. */
@Data
public class RowGroupResponse {

    /** Option id, or null for the "no value" group. */
    private String key;
    private String label;
    private String color;
    private List<RowResponse> rows = new ArrayList<>();

    public static RowGroupResponse of(String key, String label, String color) {
        RowGroupResponse g = new RowGroupResponse();
        g.key = key;
        g.label = label;
        g.color = color;
        return g;
    }
}
