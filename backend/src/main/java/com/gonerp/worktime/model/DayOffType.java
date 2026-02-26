package com.gonerp.worktime.model;

import com.gonerp.common.BaseModel;
import com.gonerp.organization.model.Organization;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "wt_day_off_types", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"name", "organization_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DayOffType extends BaseModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 20)
    @Builder.Default
    private String color = "#42A5F5";

    @Column(name = "is_paid", nullable = false, columnDefinition = "boolean default true")
    @Builder.Default
    private boolean isPaid = true;

    @Column(name = "default_quota", nullable = false)
    @Builder.Default
    private double defaultQuota = 20.0;

    @Column(name = "display_order", nullable = false)
    @Builder.Default
    private int displayOrder = 0;

    @Column(nullable = false, columnDefinition = "boolean default true")
    @Builder.Default
    private boolean active = true;
}
