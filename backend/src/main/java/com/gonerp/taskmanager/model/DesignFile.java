package com.gonerp.taskmanager.model;

import com.gonerp.common.BaseModel;
import com.gonerp.taskmanager.model.enums.DesignFileCategory;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "tm_design_files")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DesignFile extends BaseModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String url;

    @Column(name = "file_type")
    private String fileType;

    @Enumerated(EnumType.STRING)
    @Column(name = "file_category", nullable = false, length = 10)
    private DesignFileCategory fileCategory;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "design_detail_id", nullable = false)
    private DesignDetail designDetail;
}
