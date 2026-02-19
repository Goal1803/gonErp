package com.gonerp.taskmanager.dto;

import com.gonerp.taskmanager.model.BoardColumn;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ColumnResponse {
    private Long id;
    private String title;
    private int position;
    private Long boardId;
    private List<CardSummaryResponse> cards;

    public static ColumnResponse from(BoardColumn column) {
        return ColumnResponse.builder()
                .id(column.getId())
                .title(column.getTitle())
                .position(column.getPosition())
                .boardId(column.getBoard().getId())
                .cards(column.getCards().stream().map(CardSummaryResponse::from).toList())
                .build();
    }
}
