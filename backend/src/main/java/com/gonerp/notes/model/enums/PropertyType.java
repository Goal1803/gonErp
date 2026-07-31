package com.gonerp.notes.model.enums;

/**
 * Column kinds a note database can hold. The stored value is always a string;
 * each type defines how that string is interpreted (see NoteValueCodec).
 */
public enum PropertyType {
    /** Free text. */
    TEXT,
    /** Decimal number. */
    NUMBER,
    /** One option id from the property's option list. */
    SELECT,
    /** Comma-separated option ids. */
    MULTI_SELECT,
    /** ISO-8601 date (yyyy-MM-dd). */
    DATE,
    /** "true" / "false". */
    CHECKBOX,
    /** Comma-separated user ids. */
    PERSON,
    /** Absolute URL. */
    URL
}
