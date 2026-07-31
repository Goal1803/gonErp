package com.gonerp.notes.dto;

import lombok.Data;

import java.util.Map;

@Data
public class RowValueRequest {

    /** propertyId -> raw value; null or "" clears the cell. */
    private Map<String, String> values;
}
