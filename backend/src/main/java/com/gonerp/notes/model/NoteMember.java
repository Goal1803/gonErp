package com.gonerp.notes.model;

import com.gonerp.common.BaseModel;
import com.gonerp.notes.model.enums.NotePermission;
import com.gonerp.usermanager.model.User;
import jakarta.persistence.*;
import lombok.*;

/**
 * A share. Grants access to the note <em>and everything nested under it</em>,
 * the way Notion propagates page permissions downwards.
 */
@Entity
@Table(name = "nt_note_members", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"note_id", "user_id"})
}, indexes = {
        @Index(name = "idx_nt_members_user", columnList = "user_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NoteMember extends BaseModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "note_id", nullable = false)
    private Note note;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private NotePermission permission = NotePermission.EDIT;
}
