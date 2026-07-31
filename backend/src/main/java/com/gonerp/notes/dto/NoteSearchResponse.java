package com.gonerp.notes.dto;

import com.gonerp.notes.model.Note;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class NoteSearchResponse {

    private Long id;
    private String title;
    private String icon;
    private String snippet;
    private String breadcrumb;
    private LocalDateTime lastUpdatedAt;

    public static NoteSearchResponse from(Note n, String snippet, String breadcrumb) {
        NoteSearchResponse r = new NoteSearchResponse();
        r.id = n.getId();
        r.title = n.getTitle();
        r.icon = n.getIcon();
        r.snippet = snippet;
        r.breadcrumb = breadcrumb;
        r.lastUpdatedAt = n.getLastUpdatedAt();
        return r;
    }
}
