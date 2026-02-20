package com.gonerp.taskmanager.model;

import com.gonerp.common.BaseModel;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "tm_product_types")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductType extends BaseModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 200)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;
}
