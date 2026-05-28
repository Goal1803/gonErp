package com.gonerp.taskmanager.repository;

import com.gonerp.taskmanager.model.Card;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface CardRepository extends JpaRepository<Card, Long>, JpaSpecificationExecutor<Card> {

    List<Card> findByColumnIdOrderByPositionAsc(Long columnId);

    // Shift positions of cards in a column whose position >= :from by :delta.
    // Used by drag reorder to open/close a slot without sending the full card
    // list (which the client no longer holds when cards are loaded lazily).
    @Modifying
    @Query("UPDATE Card c SET c.position = c.position + :delta " +
            "WHERE c.column.id = :columnId AND c.position >= :from")
    void shiftPositionsFrom(@Param("columnId") Long columnId,
                            @Param("from") int from,
                            @Param("delta") int delta);

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

    // Search cards in a board by name (includes archived if requested)
    @Query("SELECT c FROM Card c WHERE c.column.board.id = :boardId " +
            "AND LOWER(c.name) LIKE LOWER(CONCAT('%', :query, '%')) " +
            "AND (:includeArchived = true OR c.archived = false) " +
            "ORDER BY c.archived ASC, c.lastUpdatedAt DESC")
    List<Card> searchByBoardAndName(@Param("boardId") Long boardId,
                                    @Param("query") String query,
                                    @Param("includeArchived") boolean includeArchived);

    // Find non-archived cards in specific columns older than a cutoff date (for auto-archive)
    @Query("SELECT c FROM Card c WHERE c.column.id IN :columnIds " +
            "AND c.archived = false AND c.lastUpdatedAt < :cutoff")
    List<Card> findArchiveCandidates(@Param("columnIds") List<Long> columnIds,
                                     @Param("cutoff") LocalDateTime cutoff);
}
