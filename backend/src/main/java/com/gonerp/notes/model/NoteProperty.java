package com.gonerp.notes.model;

import com.gonerp.common.BaseModel;
import com.gonerp.notes.model.enums.PropertyType;
import com.gonerp.taskmanager.model.StringListConverter;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

/**
 * One column of a note database. The database itself is just a {@link Note}
 * with {@code database = true}; its rows are that note's children.
 */
@Entity
@Table(name = "nt_properties", indexes = {
        @Index(name = "idx_nt_props_db", columnList = "database_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NoteProperty extends BaseModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "database_id", nullable = false)
    private Note database;

    @Column(nullable = false, length = 200)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private PropertyType type = PropertyType.TEXT;

    @Column(nullable = false)
    @Builder.Default
    private int position = 0;

    /**
     * Choices for SELECT / MULTI_SELECT, stored as "id:label:color" triples so a
     * rename never breaks the values that point at them.
     */
    @Column(columnDefinition = "TEXT")
    @Convert(converter = StringListConverter.class)
    @Builder.Default
    private List<String> options = new ArrayList<>();

    /** Number formatting hint: PLAIN / INTEGER / MONEY / PERCENT. */
    @Column(name = "number_format", length = 20)
    private String numberFormat;
}
