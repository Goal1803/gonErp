package com.gonerp.finance.model;

import com.gonerp.common.BaseModel;
import com.gonerp.organization.model.Organization;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "fin_currencies", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"organization_id", "code"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FinanceCurrency extends BaseModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @Column(nullable = false, length = 10)
    private String code;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 5)
    private String symbol;

    @Column(name = "is_base", nullable = false, columnDefinition = "boolean default false")
    @Builder.Default
    private boolean base = false;

    @Column(nullable = false, columnDefinition = "boolean default true")
    @Builder.Default
    private boolean active = true;
}
