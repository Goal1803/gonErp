package com.gonerp.finance.model;

import com.gonerp.common.BaseModel;
import com.gonerp.finance.model.enums.FinanceRole;
import com.gonerp.organization.model.Organization;
import com.gonerp.usermanager.model.User;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "fin_user_roles", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"organization_id", "user_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FinanceUserRole extends BaseModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "finance_role", nullable = false, length = 50)
    private FinanceRole financeRole;
}
