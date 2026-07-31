package com.gonerp.notes.model;

import com.gonerp.common.BaseModel;
import jakarta.persistence.*;
import lombok.*;

/**
 * One cell: the value of {@code property} for the row note {@code row}.
 * Always stored as text — {@link com.gonerp.notes.service.NoteValueCodec}
 * knows how to read it back for each property type.
 */
@Entity
@Table(name = "nt_values", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"row_id", "property_id"})
}, indexes = {
        @Index(name = "idx_nt_values_row", columnList = "row_id"),
        @Index(name = "idx_nt_values_prop", columnList = "property_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NoteValue extends BaseModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "row_id", nullable = false)
    private Note row;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "property_id", nullable = false)
    private NoteProperty property;

    @Column(name = "value_text", columnDefinition = "TEXT")
    private String value;
}
