package com.gonerp.notes.model;

import com.gonerp.common.BaseModel;
import com.gonerp.organization.model.Organization;
import com.gonerp.usermanager.model.User;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "nt_notes", indexes = {
        @Index(name = "idx_nt_notes_parent", columnList = "parent_id"),
        @Index(name = "idx_nt_notes_owner", columnList = "owner_id"),
        @Index(name = "idx_nt_notes_org", columnList = "organization_id"),
        @Index(name = "idx_nt_notes_path", columnList = "path")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Note extends BaseModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 500)
    @Builder.Default
    private String title = "Untitled";

    /** Emoji shown next to the title in the sidebar. */
    @Column(length = 20)
    private String icon;

    @Column(name = "cover_color", length = 20)
    private String coverColor;

    /** TipTap/ProseMirror document, stored as JSON text. */
    @Column(columnDefinition = "TEXT")
    private String content;

    /** Flattened text of {@link #content}, kept only so search can use a plain LIKE. */
    @Column(name = "plain_text", columnDefinition = "TEXT")
    private String plainText;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Note parent;

    /**
     * Materialised ancestor chain ending with this note's own id, e.g. {@code /3/17/42/}.
     * Lets a single LIKE answer "is X inside the sub-tree of Y", which is what both the
     * permission check and the trash cascade need. Rebuilt whenever a note moves.
     */
    @Column(length = 1000)
    private String path;

    @Column(nullable = false)
    @Builder.Default
    private int position = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id")
    private Organization organization;

    /** In the trash. Descendants are archived together with their root. */
    @Column(nullable = false, columnDefinition = "boolean default false")
    @Builder.Default
    private boolean archived = false;

    /** Set on the note the user actually deleted, null on descendants dragged along. */
    @Column(name = "archived_root", nullable = false, columnDefinition = "boolean default false")
    @Builder.Default
    private boolean archivedRoot = false;

    /** Usable as a starting point for new notes; hidden from the normal tree. */
    @Column(name = "is_template", nullable = false, columnDefinition = "boolean default false")
    @Builder.Default
    private boolean template = false;

    @Column(name = "full_width", nullable = false, columnDefinition = "boolean default false")
    @Builder.Default
    private boolean fullWidth = false;

    /** Locked notes stay read-only even for users who could otherwise edit. */
    @Column(nullable = false, columnDefinition = "boolean default false")
    @Builder.Default
    private boolean locked = false;

    /**
     * Marks this note as a database: its direct children become rows, and its
     * columns/views live in nt_properties / nt_views.
     */
    @Column(name = "is_database", nullable = false, columnDefinition = "boolean default false")
    @Builder.Default
    private boolean database = false;

    /** Set on rows — points at the database note they belong to. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "row_of_id")
    private Note rowOf;
}
