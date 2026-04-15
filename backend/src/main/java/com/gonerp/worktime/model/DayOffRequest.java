package com.gonerp.worktime.model;

import com.gonerp.common.BaseModel;
import com.gonerp.organization.model.Organization;
import com.gonerp.usermanager.model.User;
import com.gonerp.worktime.model.enums.DayOffRequestStatus;
import com.gonerp.worktime.model.enums.HalfDayType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "wt_day_off_requests")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DayOffRequest extends BaseModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "day_off_type_id", nullable = false)
    private DayOffType dayOffType;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    /** Legacy: used only when startDate == endDate. For multi-day ranges use start/endHalfDayType. */
    @Enumerated(EnumType.STRING)
    @Column(name = "half_day_type", length = 20)
    @Builder.Default
    private HalfDayType halfDayType = HalfDayType.FULL_DAY;

    /** Half-day marker on the first day of a multi-day range. null / FULL_DAY = full day. */
    @Enumerated(EnumType.STRING)
    @Column(name = "start_half_day_type", length = 20)
    private HalfDayType startHalfDayType;

    /** Half-day marker on the last day of a multi-day range. null / FULL_DAY = full day. */
    @Enumerated(EnumType.STRING)
    @Column(name = "end_half_day_type", length = 20)
    private HalfDayType endHalfDayType;

    @Column(name = "total_days")
    private double totalDays;

    @Column(columnDefinition = "TEXT")
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private DayOffRequestStatus status = DayOffRequestStatus.PENDING;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by")
    private User reviewedBy;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "review_comment", columnDefinition = "TEXT")
    private String reviewComment;
}
