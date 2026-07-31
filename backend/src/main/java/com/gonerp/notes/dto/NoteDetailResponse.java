package com.gonerp.notes.dto;

import com.gonerp.notes.model.Note;
import com.gonerp.taskmanager.dto.UserSummaryResponse;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class NoteDetailResponse {

    private Long id;
    private String title;
    private String icon;
    private String coverColor;
    private String content;
    private Long parentId;
    private boolean template;
    private boolean fullWidth;
    private boolean locked;
    private boolean favorite;
    private boolean archived;
    private boolean database;
    /** VIEW / EDIT / OWNER — what the caller may do with this note. */
    private String permission;
    private UserSummaryResponse owner;
    private LocalDateTime createdAt;
    private LocalDateTime lastUpdatedAt;
    private String lastUpdatedBy;

    /** Root-to-parent chain, for the header breadcrumb. */
    private List<NoteRefResponse> breadcrumb = new ArrayList<>();
    private List<NoteRefResponse> children = new ArrayList<>();
    private List<NoteRefResponse> backlinks = new ArrayList<>();
    private List<NoteMemberResponse> members = new ArrayList<>();

    public static NoteDetailResponse from(Note n) {
        NoteDetailResponse r = new NoteDetailResponse();
        r.id = n.getId();
        r.title = n.getTitle();
        r.icon = n.getIcon();
        r.coverColor = n.getCoverColor();
        r.content = n.getContent();
        r.parentId = n.getParent() == null ? null : n.getParent().getId();
        r.template = n.isTemplate();
        r.fullWidth = n.isFullWidth();
        r.locked = n.isLocked();
        r.archived = n.isArchived();
        r.database = n.isDatabase();
        r.owner = UserSummaryResponse.from(n.getOwner());
        r.createdAt = n.getCreatedAt();
        r.lastUpdatedAt = n.getLastUpdatedAt();
        r.lastUpdatedBy = n.getLastUpdatedBy();
        return r;
    }
}
