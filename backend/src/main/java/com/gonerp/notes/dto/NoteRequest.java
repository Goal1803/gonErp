package com.gonerp.notes.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class NoteRequest {

    @Size(max = 500)
    private String title;

    @Size(max = 20)
    private String icon;

    @Size(max = 20)
    private String coverColor;

    /** TipTap document JSON. Null means "leave untouched". */
    private String content;

    private Long parentId;

    private Boolean template;

    private Boolean fullWidth;

    private Boolean locked;

    /** Copy the body of this note/template into the new one. */
    private Long fromTemplateId;
}
