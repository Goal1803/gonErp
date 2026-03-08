package com.gonerp.worktime.model;

import com.gonerp.common.BaseModel;
import com.gonerp.usermanager.model.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalTime;
import java.time.ZoneId;

@Entity
@Table(name = "wt_user_configs", uniqueConstraints = {
        @UniqueConstraint(columnNames = "user_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserWorkTimeConfig extends BaseModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "timezone_id", nullable = false, length = 50)
    @Builder.Default
    private String timezoneId = "Asia/Ho_Chi_Minh";

    @Column(name = "work_start_time", nullable = false)
    @Builder.Default
    private LocalTime workStartTime = LocalTime.of(9, 0);

    @Column(name = "work_end_time", nullable = false)
    @Builder.Default
    private LocalTime workEndTime = LocalTime.of(18, 0);

    @Column(name = "daily_working_hours", nullable = false)
    @Builder.Default
    private double dailyWorkingHours = 8.0;

    @Column(name = "weekly_working_hours", nullable = false)
    @Builder.Default
    private double weeklyWorkingHours = 40.0;

    public ZoneId getZoneId() {
        try { return ZoneId.of(timezoneId); }
        catch (Exception e) { return ZoneId.of("Asia/Ho_Chi_Minh"); }
    }
}
