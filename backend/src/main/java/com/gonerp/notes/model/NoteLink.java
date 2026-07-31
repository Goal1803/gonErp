package com.gonerp.notes.model;

import jakarta.persistence.*;
import lombok.*;

/**
 * One @mention of {@code target} inside {@code source}. Rebuilt from the document
 * on every content save, so the reverse direction gives us backlinks for free.
 */
@Entity
@Table(name = "nt_note_links", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"source_id", "target_id"})
}, indexes = {
        @Index(name = "idx_nt_links_target", columnList = "target_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NoteLink {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_id", nullable = false)
    private Note source;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_id", nullable = false)
    private Note target;
}
