package com.gonerp.worktime.model;

import com.gonerp.common.BaseModel;
import com.gonerp.organization.model.Organization;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "wt_public_holidays")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PublicHoliday extends BaseModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    /** Start date of the holiday. Single-day holidays keep endDate null. */
    @Column(name = "holiday_date", nullable = false)
    private LocalDate holidayDate;

    /** End date (inclusive) for multi-day holidays. Null = single-day. */
    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(length = 20)
    private String color;

    @Column(name = "is_recurring", nullable = false, columnDefinition = "boolean default false")
    @Builder.Default
    private boolean isRecurring = false;
}
