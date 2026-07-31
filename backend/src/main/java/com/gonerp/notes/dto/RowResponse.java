package com.gonerp.notes.dto;

import com.gonerp.notes.model.Note;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/** One database row: the underlying note plus its cell values by property id. */
@Data
public class RowResponse {

    private Long id;
    private String title;
    private String icon;
    private LocalDateTime lastUpdatedAt;
    /** propertyId -> stored value (null when unset). */
    private Map<String, String> values = new LinkedHashMap<>();

    public static RowResponse from(Note n) {
        RowResponse r = new RowResponse();
        r.id = n.getId();
        r.title = n.getTitle();
        r.icon = n.getIcon();
        r.lastUpdatedAt = n.getLastUpdatedAt();
        return r;
    }
}
