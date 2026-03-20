package com.gonerp.finance.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SteuerberaterCommentRequest {
    private Long transactionId;
    @NotBlank(message = "Comment text is required")
    private String commentText;
    @NotBlank(message = "Author name is required")
    private String authorName;
}
