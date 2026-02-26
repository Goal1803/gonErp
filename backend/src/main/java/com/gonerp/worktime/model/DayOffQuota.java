package com.gonerp.worktime.model;

import com.gonerp.common.BaseModel;
import com.gonerp.usermanager.model.User;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "wt_day_off_quotas", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "day_off_type_id", "year"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DayOffQuota extends BaseModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "day_off_type_id", nullable = false)
    private DayOffType dayOffType;

    @Column(nullable = false)
    private int year;

    @Column(name = "total_days", nullable = false)
    @Builder.Default
    private double totalDays = 0;

    @Column(name = "used_days", nullable = false)
    @Builder.Default
    private double usedDays = 0;

    @Column(name = "carried_over_days", nullable = false)
    @Builder.Default
    private double carriedOverDays = 0;
}
