package com.gonerp.taskmanager.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReactionInfo {
    private int count;
    private boolean reacted;
    @Builder.Default
    private List<UserSummaryResponse> users = new ArrayList<>();
}
