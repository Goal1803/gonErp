package com.gonerp.taskmanager.repository;

import com.gonerp.taskmanager.model.CardActivity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface CardActivityRepository extends JpaRepository<CardActivity, Long> {

    // Count distinct cards that went through "Need to Fix" in a board within date range
    @Query("SELECT COUNT(DISTINCT ca.card.id) FROM CardActivity ca " +
            "WHERE ca.card.column.board.id = :boardId " +
            "AND ca.action LIKE '%Need to Fix%' " +
            "AND ca.createdAt >= :startDate AND ca.createdAt < :endDate")
    long countRejectedInBoardBetween(@Param("boardId") Long boardId,
                                      @Param("startDate") LocalDateTime startDate,
                                      @Param("endDate") LocalDateTime endDate);

    // Count distinct cards that went through "Need to Fix" across multiple boards
    @Query("SELECT COUNT(DISTINCT ca.card.id) FROM CardActivity ca " +
            "WHERE ca.card.column.board.id IN :boardIds " +
            "AND ca.action LIKE '%Need to Fix%' " +
            "AND ca.createdAt >= :startDate AND ca.createdAt < :endDate")
    long countRejectedInBoardsBetween(@Param("boardIds") List<Long> boardIds,
                                       @Param("startDate") LocalDateTime startDate,
                                       @Param("endDate") LocalDateTime endDate);
}
