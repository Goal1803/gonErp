package com.gonerp.organization.model;

import com.gonerp.common.BaseModel;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "organizations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Organization extends BaseModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 200)
    private String name;

    @Column(nullable = false, unique = true, length = 200)
    private String slug;

    @Column(nullable = false, columnDefinition = "boolean default true")
    @Builder.Default
    private boolean active = true;

    @Column(name = "module_task_manager", nullable = false, columnDefinition = "boolean default true")
    @Builder.Default
    private boolean moduleTaskManager = true;

    @Column(name = "module_image_manager", nullable = false, columnDefinition = "boolean default true")
    @Builder.Default
    private boolean moduleImageManager = true;

    @Column(name = "module_designs", nullable = false, columnDefinition = "boolean default true")
    @Builder.Default
    private boolean moduleDesigns = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "org_type_id")
    private OrganizationType orgType;
}
