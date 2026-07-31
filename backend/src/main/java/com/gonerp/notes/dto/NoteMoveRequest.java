package com.gonerp.notes.dto;

import lombok.Data;

@Data
public class NoteMoveRequest {

    /** New parent, or null to move the note to the top level. */
    private Long parentId;

    /** Index among its new siblings. */
    private Integer position;
}
