package com.gonerp.notes.dto;

import com.gonerp.notes.model.NoteMember;
import com.gonerp.notes.model.enums.NotePermission;
import com.gonerp.taskmanager.dto.UserSummaryResponse;
import lombok.Data;

@Data
public class NoteMemberResponse {

    private Long id;
    private UserSummaryResponse user;
    private NotePermission permission;

    /** Id of the note the share sits on — differs from the opened note when inherited. */
    private Long noteId;
    private String noteTitle;
    private boolean inherited;

    public static NoteMemberResponse from(NoteMember m, boolean inherited) {
        NoteMemberResponse r = new NoteMemberResponse();
        r.id = m.getId();
        r.user = UserSummaryResponse.from(m.getUser());
        r.permission = m.getPermission();
        r.noteId = m.getNote().getId();
        r.noteTitle = m.getNote().getTitle();
        r.inherited = inherited;
        return r;
    }
}
