package com.gonerp.taskmanager.model;

import com.gonerp.common.BaseModel;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "tm_card_links")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CardLink extends BaseModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 2048)
    private String url;

    @Column(length = 500)
    private String title;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "card_id", nullable = false)
    private Card card;
}
