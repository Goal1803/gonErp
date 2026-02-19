package com.gonerp.taskmanager.model;

import com.gonerp.common.BaseModel;
import com.gonerp.usermanager.model.User;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "tm_card_members", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"card_id", "user_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CardMember extends BaseModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "card_id", nullable = false)
    private Card card;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
}
