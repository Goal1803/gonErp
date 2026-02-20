package com.gonerp.taskmanager.model;

import com.gonerp.common.BaseModel;
import com.gonerp.usermanager.model.User;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "tm_comment_reactions",
        uniqueConstraints = @UniqueConstraint(columnNames = {"comment_id", "user_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommentReaction extends BaseModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "reaction_type", nullable = false, length = 20)
    private String reactionType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "comment_id", nullable = false)
    private CardComment comment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
}
