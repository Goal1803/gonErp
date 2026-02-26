package com.gonerp.worktime.model;

import com.gonerp.common.BaseModel;
import com.gonerp.organization.model.Organization;
import com.gonerp.usermanager.model.User;
import com.gonerp.worktime.model.enums.TimeEntryStatus;
import com.gonerp.worktime.model.enums.WorkLocation;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "wt_time_entries", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "work_date"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TimeEntry extends BaseModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @Column(name = "work_date", nullable = false)
    private LocalDate workDate;

    @Column(name = "check_in_time")
    private LocalDateTime checkInTime;

    @Column(name = "check_out_time")
    private LocalDateTime checkOutTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private TimeEntryStatus status = TimeEntryStatus.CHECKED_IN;

    @Enumerated(EnumType.STRING)
    @Column(name = "work_location", length = 20)
    private WorkLocation workLocation;

    @Column(name = "total_work_minutes", nullable = false)
    @Builder.Default
    private int totalWorkMinutes = 0;

    @Column(name = "total_break_minutes", nullable = false)
    @Builder.Default
    private int totalBreakMinutes = 0;

    @Column(name = "overtime_minutes", nullable = false)
    @Builder.Default
    private int overtimeMinutes = 0;

    @Column(name = "is_late_arrival", nullable = false, columnDefinition = "boolean default false")
    @Builder.Default
    private boolean isLateArrival = false;

    @Column(name = "is_early_departure", nullable = false, columnDefinition = "boolean default false")
    @Builder.Default
    private boolean isEarlyDeparture = false;

    @Column(name = "daily_notes", columnDefinition = "TEXT")
    private String dailyNotes;

    @OneToMany(mappedBy = "timeEntry", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<BreakEntry> breaks = new ArrayList<>();
}
