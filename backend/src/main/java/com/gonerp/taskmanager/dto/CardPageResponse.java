package com.gonerp.taskmanager.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

/** A page of cards for one column, plus the total matching the current filter. */
@Data
@AllArgsConstructor
public class CardPageResponse {
    private List<CardSummaryResponse> content;
    private long total;
    private int page;
    private int size;
}
