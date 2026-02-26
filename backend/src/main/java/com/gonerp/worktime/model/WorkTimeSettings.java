package com.gonerp.worktime.model;

import com.gonerp.common.BaseModel;
import com.gonerp.organization.model.Organization;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalTime;

@Entity
@Table(name = "wt_settings", uniqueConstraints = {
        @UniqueConstraint(columnNames = "organization_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkTimeSettings extends BaseModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @Column(name = "daily_working_hours", nullable = false)
    @Builder.Default
    private double dailyWorkingHours = 8.0;

    @Column(name = "weekly_full_time_hours", nullable = false)
    @Builder.Default
    private double weeklyFullTimeHours = 40.0;

    @Column(name = "weekly_part_time_hours", nullable = false)
    @Builder.Default
    private double weeklyPartTimeHours = 20.0;

    @Column(name = "required_break_minutes", nullable = false)
    @Builder.Default
    private int requiredBreakMinutes = 60;

    @Column(name = "break_counts_as_work", nullable = false, columnDefinition = "boolean default false")
    @Builder.Default
    private boolean breakCountsAsWork = false;

    @Column(name = "overtime_tracking_enabled", nullable = false, columnDefinition = "boolean default true")
    @Builder.Default
    private boolean overtimeTrackingEnabled = true;

    @Column(name = "late_early_tracking_enabled", nullable = false, columnDefinition = "boolean default true")
    @Builder.Default
    private boolean lateEarlyTrackingEnabled = true;

    @Column(name = "work_start_time", nullable = false)
    @Builder.Default
    private LocalTime workStartTime = LocalTime.of(9, 0);

    @Column(name = "work_end_time", nullable = false)
    @Builder.Default
    private LocalTime workEndTime = LocalTime.of(18, 0);

    @Column(name = "carry_over_enabled", nullable = false, columnDefinition = "boolean default false")
    @Builder.Default
    private boolean carryOverEnabled = false;

    @Column(name = "max_carry_over_days", nullable = false)
    @Builder.Default
    private int maxCarryOverDays = 5;

    @Column(name = "daily_notes_enabled", nullable = false, columnDefinition = "boolean default true")
    @Builder.Default
    private boolean dailyNotesEnabled = true;

    @Column(name = "work_location_enabled", nullable = false, columnDefinition = "boolean default true")
    @Builder.Default
    private boolean workLocationEnabled = true;

    @Column(name = "auto_checkout_reminder_minutes", nullable = false)
    @Builder.Default
    private int autoCheckoutReminderMinutes = 0;
}
