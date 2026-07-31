package com.gonerp.notes.model;

import com.gonerp.common.BaseModel;
import com.gonerp.notes.model.enums.ViewType;
import com.gonerp.taskmanager.model.LongListConverter;
import com.gonerp.taskmanager.model.StringListConverter;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

/**
 * A saved way of looking at one database: which rows, in what order, laid out
 * as a table / board / gallery / list.
 */
@Entity
@Table(name = "nt_views", indexes = {
        @Index(name = "idx_nt_views_db", columnList = "database_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NoteView extends BaseModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "database_id", nullable = false)
    private Note database;

    @Column(nullable = false, length = 200)
    @Builder.Default
    private String name = "Table";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private ViewType type = ViewType.TABLE;

    @Column(nullable = false)
    @Builder.Default
    private int position = 0;

    /** Property the BOARD view makes columns from (must be SELECT). */
    @Column(name = "group_by_id")
    private Long groupById;

    /** Filters as "propertyId:operator:value" triples, all ANDed together. */
    @Column(columnDefinition = "TEXT")
    @Convert(converter = StringListConverter.class)
    @Builder.Default
    private List<String> filters = new ArrayList<>();

    /** Sorts as "propertyId:ASC|DESC", applied in order. */
    @Column(columnDefinition = "TEXT")
    @Convert(converter = StringListConverter.class)
    @Builder.Default
    private List<String> sorts = new ArrayList<>();

    /** Properties shown in this view; empty means "show all". */
    @Column(name = "visible_property_ids", columnDefinition = "TEXT")
    @Convert(converter = LongListConverter.class)
    @Builder.Default
    private List<Long> visiblePropertyIds = new ArrayList<>();
}
