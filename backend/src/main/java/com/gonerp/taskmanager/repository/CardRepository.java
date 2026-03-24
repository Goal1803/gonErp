package com.gonerp.taskmanager.repository;

import com.gonerp.taskmanager.model.Card;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface CardRepository extends JpaRepository<Card, Long> {

    List<Card> findByColumnIdOrderByPositionAsc(Long columnId);

    int countByColumnId(Long columnId);

    // Count cards created in a board within a date range
    @Query("SELECT COUNT(c) FROM Card c WHERE c.column.board.id = :boardId " +
            "AND c.createdAt >= :startDate AND c.createdAt < :endDate")
    long countCreatedInBoardBetween(@Param("boardId") Long boardId,
                                    @Param("startDate") LocalDateTime startDate,
                                    @Param("endDate") LocalDateTime endDate);

    // Count cards created by a specific user in a board within a date range
    @Query("SELECT COUNT(c) FROM Card c WHERE c.column.board.id = :boardId " +
            "AND c.createdAt >= :startDate AND c.createdAt < :endDate " +
            "AND c.createdBy = :username")
    long countCreatedInBoardByUserBetween(@Param("boardId") Long boardId,
                                          @Param("username") String username,
                                          @Param("startDate") LocalDateTime startDate,
                                          @Param("endDate") LocalDateTime endDate);

    // Count cards created across multiple boards within a date range
    @Query("SELECT COUNT(c) FROM Card c WHERE c.column.board.id IN :boardIds " +
            "AND c.createdAt >= :startDate AND c.createdAt < :endDate")
    long countCreatedInBoardsBetween(@Param("boardIds") List<Long> boardIds,
                                     @Param("startDate") LocalDateTime startDate,
                                     @Param("endDate") LocalDateTime endDate);

    // Count cards created by a specific user across multiple boards
    @Query("SELECT COUNT(c) FROM Card c WHERE c.column.board.id IN :boardIds " +
            "AND c.createdAt >= :startDate AND c.createdAt < :endDate " +
            "AND c.createdBy = :username")
    long countCreatedInBoardsByUserBetween(@Param("boardIds") List<Long> boardIds,
                                           @Param("username") String username,
                                           @Param("startDate") LocalDateTime startDate,
                                           @Param("endDate") LocalDateTime endDate);

    // Find cards created in a board within a date range
    @Query("SELECT c FROM Card c WHERE c.column.board.id = :boardId " +
            "AND c.createdAt >= :startDate AND c.createdAt < :endDate")
    List<Card> findCreatedInBoardBetween(@Param("boardId") Long boardId,
                                         @Param("startDate") LocalDateTime startDate,
                                         @Param("endDate") LocalDateTime endDate);

    // Find cards created across multiple boards within a date range
    @Query("SELECT c FROM Card c WHERE c.column.board.id IN :boardIds " +
            "AND c.createdAt >= :startDate AND c.createdAt < :endDate")
    List<Card> findCreatedInBoardsBetween(@Param("boardIds") List<Long> boardIds,
                                          @Param("startDate") LocalDateTime startDate,
                                          @Param("endDate") LocalDateTime endDate);

    // Find cards where user is a member (CardMember) in a board within date range
    @Query("SELECT DISTINCT c FROM Card c JOIN c.members cm WHERE c.column.board.id = :boardId " +
            "AND cm.user.id = :userId AND c.createdAt >= :startDate AND c.createdAt < :endDate")
    List<Card> findByMemberInBoardBetween(@Param("boardId") Long boardId,
                                           @Param("userId") Long userId,
                                           @Param("startDate") LocalDateTime startDate,
                                           @Param("endDate") LocalDateTime endDate);

    // Find cards where user is a member across multiple boards within date range
    @Query("SELECT DISTINCT c FROM Card c JOIN c.members cm WHERE c.column.board.id IN :boardIds " +
            "AND cm.user.id = :userId AND c.createdAt >= :startDate AND c.createdAt < :endDate")
    List<Card> findByMemberInBoardsBetween(@Param("boardIds") List<Long> boardIds,
                                            @Param("userId") Long userId,
                                            @Param("startDate") LocalDateTime startDate,
                                            @Param("endDate") LocalDateTime endDate);

    // Find cards in "Canceled" column with lastUpdatedAt in range for a board
    @Query("SELECT c FROM Card c WHERE c.column.board.id = :boardId " +
            "AND LOWER(c.column.title) = 'canceled' " +
            "AND c.lastUpdatedAt >= :startDate AND c.lastUpdatedAt < :endDate")
    List<Card> findCancelledInBoardBetween(@Param("boardId") Long boardId,
                                            @Param("startDate") LocalDateTime startDate,
                                            @Param("endDate") LocalDateTime endDate);

    // Find cards in "Canceled" column with lastUpdatedAt in range across multiple boards
    @Query("SELECT c FROM Card c WHERE c.column.board.id IN :boardIds " +
            "AND LOWER(c.column.title) = 'canceled' " +
            "AND c.lastUpdatedAt >= :startDate AND c.lastUpdatedAt < :endDate")
    List<Card> findCancelledInBoardsBetween(@Param("boardIds") List<Long> boardIds,
                                             @Param("startDate") LocalDateTime startDate,
                                             @Param("endDate") LocalDateTime endDate);

    // Count cards per column for a board (snapshot)
    @Query("SELECT c.column.title, COUNT(c) FROM Card c WHERE c.column.board.id = :boardId GROUP BY c.column.title")
    List<Object[]> countByColumnForBoard(@Param("boardId") Long boardId);

    // Count cards per column across multiple boards (snapshot)
    @Query("SELECT c.column.title, COUNT(c) FROM Card c WHERE c.column.board.id IN :boardIds GROUP BY c.column.title")
    List<Object[]> countByColumnForBoards(@Param("boardIds") List<Long> boardIds);
}
