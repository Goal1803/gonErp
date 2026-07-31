package com.gonerp.notes.dto;

import com.gonerp.notes.model.enums.ViewType;
import lombok.Data;

import java.util.List;

@Data
public class ViewRequest {

    private String name;

    private ViewType type;

    private Long groupById;

    /** "propertyId:operator:value", ANDed. */
    private List<String> filters;

    /** "propertyId:ASC|DESC", applied in order. */
    private List<String> sorts;

    /** Empty means every property is visible. */
    private List<Long> visiblePropertyIds;

    private Integer position;
}
