package com.gonerp.notes.model.enums;

/**
 * Access level a user has on a note sub-tree.
 * OWNER is not stored — it is derived from {@code Note.owner}.
 */
public enum NotePermission {
    VIEW,
    EDIT
}
