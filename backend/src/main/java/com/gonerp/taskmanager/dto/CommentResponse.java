package com.gonerp.taskmanager.dto;

import com.gonerp.taskmanager.model.CardComment;
import com.gonerp.taskmanager.model.CommentReaction;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class CommentResponse {
    private Long id;
    private String content;
    private UserSummaryResponse author;
    private LocalDateTime createdAt;
    private Long parentId;
    @Builder.Default
    private List<CommentResponse> replies = List.of();
    @Builder.Default
    private List<String> imageUrls = List.of();
    @Builder.Default
    private Map<String, ReactionInfo> reactions = new HashMap<>();
    @Builder.Default
    private List<UserSummaryResponse> mentionedUsers = List.of();

    public static CommentResponse from(CardComment comment) {
        return from(comment, null);
    }

    public static CommentResponse from(CardComment comment, Long currentUserId) {
        CommentResponse.CommentResponseBuilder builder = CommentResponse.builder()
                .id(comment.getId())
                .content(comment.getContent())
                .author(UserSummaryResponse.from(comment.getAuthor()))
                .createdAt(comment.getCreatedAt())
                .imageUrls(comment.getImageUrls() != null ? comment.getImageUrls() : List.of())
                .parentId(comment.getParent() != null ? comment.getParent().getId() : null);

        // Build reactions map
        Map<String, ReactionInfo> reactionsMap = new HashMap<>();
        if (comment.getReactions() != null) {
            for (CommentReaction r : comment.getReactions()) {
                ReactionInfo info = reactionsMap.computeIfAbsent(r.getReactionType(),
                        k -> ReactionInfo.builder().build());
                info.setCount(info.getCount() + 1);
                info.getUsers().add(UserSummaryResponse.from(r.getUser()));
                if (currentUserId != null && r.getUser().getId().equals(currentUserId)) {
                    info.setReacted(true);
                }
            }
        }
        builder.reactions(reactionsMap);

        // Build replies (only for top-level comments)
        if (comment.getParent() == null && comment.getReplies() != null && !comment.getReplies().isEmpty()) {
            builder.replies(comment.getReplies().stream()
                    .map(reply -> CommentResponse.from(reply, currentUserId))
                    .toList());
        }

        return builder.build();
    }
}
