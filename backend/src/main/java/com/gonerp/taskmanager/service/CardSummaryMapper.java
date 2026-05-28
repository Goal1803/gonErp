package com.gonerp.taskmanager.service;

import com.gonerp.taskmanager.dto.CardSummaryResponse;
import com.gonerp.taskmanager.model.Card;
import com.gonerp.taskmanager.repository.CardAttachmentRepository;
import com.gonerp.taskmanager.repository.CardCommentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Builds {@link CardSummaryResponse} lists with comment/attachment counts fetched
 * in two batch COUNT queries, instead of loading each card's full collections.
 */
@Component
@RequiredArgsConstructor
public class CardSummaryMapper {

    private final CardCommentRepository cardCommentRepository;
    private final CardAttachmentRepository cardAttachmentRepository;

    public List<CardSummaryResponse> toSummaries(List<Card> cards) {
        if (cards == null || cards.isEmpty()) return List.of();
        List<Long> ids = cards.stream().map(Card::getId).toList();
        Map<Long, Integer> comments = toMap(cardCommentRepository.countByCardIds(ids));
        Map<Long, Integer> attachments = toMap(cardAttachmentRepository.countByCardIds(ids));
        return cards.stream()
                .map(c -> CardSummaryResponse.from(c,
                        comments.getOrDefault(c.getId(), 0),
                        attachments.getOrDefault(c.getId(), 0)))
                .toList();
    }

    private Map<Long, Integer> toMap(List<Object[]> rows) {
        Map<Long, Integer> map = new HashMap<>();
        for (Object[] row : rows) {
            map.put((Long) row[0], ((Number) row[1]).intValue());
        }
        return map;
    }
}
