package com.gonerp.notes.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/** Schema of a database: its columns and its saved views. */
@Data
public class DatabaseResponse {

    private Long id;
    private String title;
    private List<PropertyResponse> properties = new ArrayList<>();
    private List<ViewResponse> views = new ArrayList<>();
    private String permission;
}
