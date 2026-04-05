package com.gonerp.taskmanager.model;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.ArrayList;
import java.util.List;

@Converter
public class LongListConverter implements AttributeConverter<List<Long>, String> {

    private static final ObjectMapper mapper = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(List<Long> list) {
        if (list == null || list.isEmpty()) return null;
        try {
            return mapper.writeValueAsString(list);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public List<Long> convertToEntityAttribute(String json) {
        if (json == null || json.isBlank()) return new ArrayList<>();
        try {
            return mapper.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }
}
