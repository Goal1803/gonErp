package com.gonerp.taskmanager.dto;

import com.gonerp.taskmanager.model.CardComment;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class CommentResponse {
    private Long id;
    private String content;
    private UserSummaryResponse author;
    private LocalDateTime createdAt;

    public static CommentResponse from(CardComment comment) {
        return CommentResponse.builder()
                .id(comment.getId())
                .content(comment.getContent())
                .author(UserSummaryResponse.from(comment.getAuthor()))
                .createdAt(comment.getCreatedAt())
                .build();
    }
}
