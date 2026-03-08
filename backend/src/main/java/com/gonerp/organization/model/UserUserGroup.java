package com.gonerp.organization.model;

import com.gonerp.common.BaseModel;
import com.gonerp.organization.model.enums.GroupRole;
import com.gonerp.usermanager.model.User;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "user_user_groups", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "user_group_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserUserGroup extends BaseModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_group_id", nullable = false)
    private UserGroup userGroup;

    @Enumerated(EnumType.STRING)
    @Column(name = "group_role", nullable = false, length = 20)
    @Builder.Default
    private GroupRole groupRole = GroupRole.MEMBER;
}
