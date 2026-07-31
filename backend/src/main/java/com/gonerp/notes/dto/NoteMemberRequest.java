package com.gonerp.notes.dto;

import com.gonerp.notes.model.enums.NotePermission;
import lombok.Data;

@Data
public class NoteMemberRequest {

    private Long userId;

    private NotePermission permission = NotePermission.EDIT;
}
