package com.gonerp.organization.model;

import com.gonerp.common.BaseModel;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "user_groups", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"name", "org_type_id"}),
    @UniqueConstraint(columnNames = {"name", "organization_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserGroup extends BaseModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "org_type_id")
    private OrganizationType orgType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id")
    private Organization organization;
}
