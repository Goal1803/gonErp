package com.gonerp.taskmanager.model;

import com.gonerp.common.BaseModel;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "tm_labels")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CardLabel extends BaseModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String color = "#2E7D32";

    @Column(length = 20, columnDefinition = "varchar(20) default '#ffffff'")
    @Builder.Default
    private String textColor = "#ffffff";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "board_id", nullable = false)
    private Board board;
}
