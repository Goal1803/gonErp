package com.gonerp.ecommerce.model;

import com.gonerp.common.BaseModel;
import com.gonerp.ecommerce.model.enums.StoreRole;
import com.gonerp.usermanager.model.User;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "ecom_store_members", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"store_id", "user_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EcomStoreMember extends BaseModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false)
    private EcomStore store;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "store_role", nullable = false, length = 50)
    private StoreRole storeRole;
}
