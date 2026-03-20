package com.gonerp.finance.dto;

import com.gonerp.finance.model.FinanceSteuerberaterComment;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class SteuerberaterCommentResponse {
    private Long id;
    private Long transactionId;
    private String commentText;
    private String authorName;
    private LocalDateTime createdAt;

    public static SteuerberaterCommentResponse from(FinanceSteuerberaterComment entity) {
        return SteuerberaterCommentResponse.builder()
                .id(entity.getId())
                .transactionId(entity.getTransaction() != null ? entity.getTransaction().getId() : null)
                .commentText(entity.getCommentText())
                .authorName(entity.getAuthorName())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
