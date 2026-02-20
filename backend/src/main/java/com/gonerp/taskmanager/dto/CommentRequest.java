package com.gonerp.taskmanager.dto;

import lombok.Data;

import java.util.List;

@Data
public class CommentRequest {
    private String content;
    private Long parentCommentId;
    private List<String> imageUrls;
    private List<Long> mentionedUserIds;
}
