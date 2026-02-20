package com.gonerp.taskmanager.model;

import com.gonerp.common.BaseModel;
import com.gonerp.usermanager.model.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "tm_design_details")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DesignDetail extends BaseModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "card_id", nullable = false, unique = true)
    private Card card;

    @Column(name = "png_file_url")
    private String pngFileUrl;

    @Column(name = "png_file_name")
    private String pngFileName;

    @Column(name = "psd_file_url")
    private String psdFileUrl;

    @Column(name = "psd_file_name")
    private String psdFileName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id")
    private User seller;

    @ManyToMany
    @JoinTable(
            name = "tm_design_designers",
            joinColumns = @JoinColumn(name = "design_detail_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    @Builder.Default
    private List<User> designers = new ArrayList<>();

    @Column(name = "approval_date")
    private LocalDateTime approvalDate;

    @ManyToMany
    @JoinTable(
            name = "tm_design_product_types",
            joinColumns = @JoinColumn(name = "design_detail_id"),
            inverseJoinColumns = @JoinColumn(name = "product_type_id")
    )
    @Builder.Default
    private List<ProductType> productTypes = new ArrayList<>();

    @ManyToMany
    @JoinTable(
            name = "tm_design_niches",
            joinColumns = @JoinColumn(name = "design_detail_id"),
            inverseJoinColumns = @JoinColumn(name = "niche_id")
    )
    @Builder.Default
    private List<Niche> niches = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "occasion_id")
    private Occasion occasion;

    @Column(nullable = false)
    @Builder.Default
    private boolean custom = false;

    @OneToMany(mappedBy = "designDetail", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<DesignMockup> mockups = new ArrayList<>();
}
