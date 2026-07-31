package com.gonerp.notes.dto;

import com.gonerp.notes.model.enums.PropertyType;
import lombok.Data;

import java.util.List;

@Data
public class PropertyRequest {

    private String name;

    private PropertyType type;

    /** "id:label:color" triples for SELECT / MULTI_SELECT. */
    private List<String> options;

    private String numberFormat;

    private Integer position;
}
