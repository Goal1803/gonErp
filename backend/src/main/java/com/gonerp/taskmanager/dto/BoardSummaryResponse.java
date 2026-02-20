package com.gonerp.taskmanager.dto;

import com.gonerp.taskmanager.model.Board;
import com.gonerp.taskmanager.model.enums.BoardType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class BoardSummaryResponse {
    private Long id;
    private String name;
    private String description;
    private String coverColor;
    private BoardType boardType;
    private String ownerName;
    private Long ownerId;
    private int memberCount;
    private int columnCount;
    private LocalDateTime createdAt;

    public static BoardSummaryResponse from(Board board) {
        return BoardSummaryResponse.builder()
                .id(board.getId())
                .name(board.getName())
                .description(board.getDescription())
                .coverColor(board.getCoverColor())
                .boardType(board.getBoardType())
                .ownerName(board.getOwner().getUserName())
                .ownerId(board.getOwner().getId())
                .memberCount(board.getMembers().size())
                .columnCount(board.getColumns().size())
                .createdAt(board.getCreatedAt())
                .build();
    }
}
