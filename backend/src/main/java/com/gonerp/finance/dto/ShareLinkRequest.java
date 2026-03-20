package com.gonerp.finance.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ShareLinkRequest {
    @NotNull(message = "Monthly report ID is required")
    private Long monthlyReportId;
    private String recipientName;
    private String recipientEmail;
    private LocalDateTime expiresAt;
}
