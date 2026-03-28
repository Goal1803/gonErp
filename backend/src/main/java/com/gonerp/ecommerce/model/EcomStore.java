package com.gonerp.ecommerce.model;

import com.gonerp.common.BaseModel;
import com.gonerp.ecommerce.model.enums.SalesChannel;
import com.gonerp.organization.model.Organization;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "ecom_stores")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EcomStore extends BaseModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @Column(nullable = false, length = 200)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "sales_channel", length = 50)
    private SalesChannel salesChannel;

    @Column(nullable = false, length = 10)
    @Builder.Default
    private String currency = "EUR";

    @Column(name = "store_url", length = 500)
    private String storeUrl;

    @Column(nullable = false, columnDefinition = "boolean default true")
    @Builder.Default
    private boolean active = true;
}
