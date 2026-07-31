package com.gonerp.notes.dto;

import com.gonerp.notes.model.Note;
import lombok.Data;

/** Lightweight pointer to a note: breadcrumbs, child lists, backlinks, mention picker. */
@Data
public class NoteRefResponse {

    private Long id;
    private String title;
    private String icon;

    public static NoteRefResponse from(Note n) {
        NoteRefResponse r = new NoteRefResponse();
        r.id = n.getId();
        r.title = n.getTitle();
        r.icon = n.getIcon();
        return r;
    }
}
