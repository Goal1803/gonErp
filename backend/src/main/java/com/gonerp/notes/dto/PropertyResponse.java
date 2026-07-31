package com.gonerp.notes.dto;

import com.gonerp.notes.model.NoteProperty;
import com.gonerp.notes.model.enums.PropertyType;
import lombok.Data;

import java.util.List;

@Data
public class PropertyResponse {

    private Long id;
    private String name;
    private PropertyType type;
    private int position;
    private List<String> options;
    private String numberFormat;

    public static PropertyResponse from(NoteProperty p) {
        PropertyResponse r = new PropertyResponse();
        r.id = p.getId();
        r.name = p.getName();
        r.type = p.getType();
        r.position = p.getPosition();
        r.options = p.getOptions();
        r.numberFormat = p.getNumberFormat();
        return r;
    }
}
