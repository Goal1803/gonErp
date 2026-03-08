package com.gonerp.worktime.model;

import com.gonerp.common.BaseModel;
import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "wt_breaks")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BreakEntry extends BaseModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "time_entry_id", nullable = false)
    private TimeEntry timeEntry;

    @Column(name = "start_time", nullable = false, columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private OffsetDateTime startTime;

    @Column(name = "end_time", columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private OffsetDateTime endTime;

    @Column(name = "duration_minutes", nullable = false)
    @Builder.Default
    private int durationMinutes = 0;
}
