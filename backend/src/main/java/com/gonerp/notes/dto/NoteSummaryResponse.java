package com.gonerp.notes.dto;

import com.gonerp.notes.model.Note;
import com.gonerp.taskmanager.dto.UserSummaryResponse;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Sidebar/tree node. Carries no body, so the whole tree stays cheap to ship. */
@Data
public class NoteSummaryResponse {

    private Long id;
    private String title;
    private String icon;
    private String coverColor;
    private Long parentId;
    private int position;
    private boolean template;
    private boolean favorite;
    private boolean shared;
    private boolean archived;
    private boolean database;
    private String permission;
    private UserSummaryResponse owner;
    private LocalDateTime lastUpdatedAt;
    private List<NoteSummaryResponse> children = new ArrayList<>();

    public static NoteSummaryResponse from(Note n) {
        NoteSummaryResponse r = new NoteSummaryResponse();
        r.id = n.getId();
        r.title = n.getTitle();
        r.icon = n.getIcon();
        r.coverColor = n.getCoverColor();
        r.parentId = n.getParent() == null ? null : n.getParent().getId();
        r.position = n.getPosition();
        r.template = n.isTemplate();
        r.archived = n.isArchived();
        r.database = n.isDatabase();
        r.owner = UserSummaryResponse.from(n.getOwner());
        r.lastUpdatedAt = n.getLastUpdatedAt();
        return r;
    }
}
