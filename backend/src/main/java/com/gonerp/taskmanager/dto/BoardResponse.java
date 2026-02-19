package com.gonerp.taskmanager.dto;

import com.gonerp.taskmanager.model.Board;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class BoardResponse {
    private Long id;
    private String name;
    private String description;
    private String coverColor;
    private UserSummaryResponse owner;
    private List<BoardMemberResponse> members;
    private List<ColumnResponse> columns;
    private List<LabelResponse> labels;

    public static BoardResponse from(Board board, List<LabelResponse> labels) {
        return BoardResponse.builder()
                .id(board.getId())
                .name(board.getName())
                .description(board.getDescription())
                .coverColor(board.getCoverColor())
                .owner(UserSummaryResponse.from(board.getOwner()))
                .members(board.getMembers().stream().map(BoardMemberResponse::from).toList())
                .columns(board.getColumns().stream().map(ColumnResponse::from).toList())
                .labels(labels)
                .build();
    }
}
