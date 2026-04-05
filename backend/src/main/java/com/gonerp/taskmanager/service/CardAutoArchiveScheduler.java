package com.gonerp.taskmanager.service;

import com.gonerp.taskmanager.model.Board;
import com.gonerp.taskmanager.model.Card;
import com.gonerp.taskmanager.repository.BoardRepository;
import com.gonerp.taskmanager.repository.CardRepository;
import com.gonerp.taskmanager.websocket.BoardEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class CardAutoArchiveScheduler {

    private final BoardRepository boardRepository;
    private final CardRepository cardRepository;
    private final BoardEventPublisher eventPublisher;

    @Scheduled(cron = "0 0 3 * * *") // Daily at 3 AM
    @Transactional
    public void autoArchiveCards() {
        List<Board> boards = boardRepository.findBoardsWithAutoArchive();
        int totalArchived = 0;

        for (Board board : boards) {
            List<Long> columnIds = board.getArchiveColumnIds();
            if (columnIds == null || columnIds.isEmpty() || board.getAutoArchiveDays() == null) continue;

            LocalDateTime cutoff = LocalDateTime.now().minusDays(board.getAutoArchiveDays());
            List<Card> candidates = cardRepository.findArchiveCandidates(columnIds, cutoff);

            for (Card card : candidates) {
                card.setArchived(true);
                card.setArchivedAt(LocalDateTime.now());
                totalArchived++;
            }

            if (!candidates.isEmpty()) {
                cardRepository.saveAll(candidates);
                // Notify connected clients to refresh
                eventPublisher.publish(board.getId(), "CARDS_AUTO_ARCHIVED", null, null,
                        "system", Map.of("count", candidates.size()));
                log.info("Auto-archived {} cards in board '{}' (id={})", candidates.size(), board.getName(), board.getId());
            }
        }

        if (totalArchived > 0) {
            log.info("Auto-archive complete: {} cards archived across {} boards", totalArchived, boards.size());
        }
    }
}
