package com.gonerp.taskmanager.model;

import com.gonerp.common.BaseModel;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "tm_design_mockups")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DesignMockup extends BaseModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String url;

    @Column(name = "file_type")
    private String fileType;

    @Column(name = "main_mockup", nullable = false)
    @Builder.Default
    private boolean mainMockup = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "design_detail_id", nullable = false)
    private DesignDetail designDetail;
}
