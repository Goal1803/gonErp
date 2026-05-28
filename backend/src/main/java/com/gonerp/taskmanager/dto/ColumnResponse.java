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
    // Total non-archived (and filter-matching) cards in this column. The `cards`
    // list may hold only the first page when the board is loaded lazily; the
    // frontend uses this to know whether more cards remain to fetch.
    private long totalCards;

    public static ColumnResponse from(BoardColumn column) {
        List<CardSummaryResponse> cards = column.getCards().stream().map(CardSummaryResponse::from).toList();
        return from(column, cards, cards.size());
    }

    public static ColumnResponse from(BoardColumn column, List<CardSummaryResponse> cards, long totalCards) {
        return ColumnResponse.builder()
                .id(column.getId())
                .title(column.getTitle())
                .position(column.getPosition())
                .boardId(column.getBoard().getId())
                .cards(cards)
                .totalCards(totalCards)
                .build();
    }
}
