package com.gonerp.taskmanager.repository;

import com.gonerp.taskmanager.model.DesignDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface DesignDetailRepository extends JpaRepository<DesignDetail, Long>, JpaSpecificationExecutor<DesignDetail> {
    Optional<DesignDetail> findByCardId(Long cardId);

    // Find completed designs (with approvalDate) in a board within date range
    @Query("SELECT dd FROM DesignDetail dd JOIN dd.card c " +
            "WHERE c.column.board.id = :boardId " +
            "AND dd.approvalDate >= :startDate AND dd.approvalDate < :endDate")
    List<DesignDetail> findCompletedInBoardBetween(@Param("boardId") Long boardId,
                                                    @Param("startDate") LocalDateTime startDate,
                                                    @Param("endDate") LocalDateTime endDate);

    // Find completed designs across multiple boards
    @Query("SELECT dd FROM DesignDetail dd JOIN dd.card c " +
            "WHERE c.column.board.id IN :boardIds " +
            "AND dd.approvalDate >= :startDate AND dd.approvalDate < :endDate")
    List<DesignDetail> findCompletedInBoardsBetween(@Param("boardIds") List<Long> boardIds,
                                                     @Param("startDate") LocalDateTime startDate,
                                                     @Param("endDate") LocalDateTime endDate);

    // Count completed designs for a specific designer in a board
    @Query("SELECT COUNT(DISTINCT dd) FROM DesignDetail dd JOIN dd.designers d JOIN dd.card c " +
            "WHERE c.column.board.id = :boardId " +
            "AND d.id = :userId " +
            "AND dd.approvalDate >= :startDate AND dd.approvalDate < :endDate")
    long countCompletedByDesignerInBoardBetween(@Param("boardId") Long boardId,
                                                @Param("userId") Long userId,
                                                @Param("startDate") LocalDateTime startDate,
                                                @Param("endDate") LocalDateTime endDate);

    // Count completed designs for a specific designer across multiple boards
    @Query("SELECT COUNT(DISTINCT dd) FROM DesignDetail dd JOIN dd.designers d JOIN dd.card c " +
            "WHERE c.column.board.id IN :boardIds " +
            "AND d.id = :userId " +
            "AND dd.approvalDate >= :startDate AND dd.approvalDate < :endDate")
    long countCompletedByDesignerInBoardsBetween(@Param("boardIds") List<Long> boardIds,
                                                  @Param("userId") Long userId,
                                                  @Param("startDate") LocalDateTime startDate,
                                                  @Param("endDate") LocalDateTime endDate);

    // Find completed designs for a specific designer in a board (for avg days calc)
    @Query("SELECT dd FROM DesignDetail dd JOIN dd.designers d JOIN dd.card c " +
            "WHERE c.column.board.id = :boardId " +
            "AND d.id = :userId " +
            "AND dd.approvalDate >= :startDate AND dd.approvalDate < :endDate")
    List<DesignDetail> findCompletedByDesignerInBoardBetween(@Param("boardId") Long boardId,
                                                             @Param("userId") Long userId,
                                                             @Param("startDate") LocalDateTime startDate,
                                                             @Param("endDate") LocalDateTime endDate);

    // Find completed designs for a specific designer across multiple boards
    @Query("SELECT dd FROM DesignDetail dd JOIN dd.designers d JOIN dd.card c " +
            "WHERE c.column.board.id IN :boardIds " +
            "AND d.id = :userId " +
            "AND dd.approvalDate >= :startDate AND dd.approvalDate < :endDate")
    List<DesignDetail> findCompletedByDesignerInBoardsBetween(@Param("boardIds") List<Long> boardIds,
                                                               @Param("userId") Long userId,
                                                               @Param("startDate") LocalDateTime startDate,
                                                               @Param("endDate") LocalDateTime endDate);

    // Count product types for completed designs in a board
    @Query("SELECT pt.name, COUNT(dd) FROM DesignDetail dd JOIN dd.card c JOIN dd.productTypes pt " +
            "WHERE c.column.board.id = :boardId " +
            "AND dd.approvalDate >= :startDate AND dd.approvalDate < :endDate " +
            "GROUP BY pt.name ORDER BY COUNT(dd) DESC")
    List<Object[]> countProductTypesInBoardBetween(@Param("boardId") Long boardId,
                                                    @Param("startDate") LocalDateTime startDate,
                                                    @Param("endDate") LocalDateTime endDate);

    // Count product types for completed designs across multiple boards
    @Query("SELECT pt.name, COUNT(dd) FROM DesignDetail dd JOIN dd.card c JOIN dd.productTypes pt " +
            "WHERE c.column.board.id IN :boardIds " +
            "AND dd.approvalDate >= :startDate AND dd.approvalDate < :endDate " +
            "GROUP BY pt.name ORDER BY COUNT(dd) DESC")
    List<Object[]> countProductTypesInBoardsBetween(@Param("boardIds") List<Long> boardIds,
                                                     @Param("startDate") LocalDateTime startDate,
                                                     @Param("endDate") LocalDateTime endDate);

    // Count niches for completed designs in a board
    @Query("SELECT n.name, COUNT(dd) FROM DesignDetail dd JOIN dd.card c JOIN dd.niches n " +
            "WHERE c.column.board.id = :boardId " +
            "AND dd.approvalDate >= :startDate AND dd.approvalDate < :endDate " +
            "GROUP BY n.name ORDER BY COUNT(dd) DESC")
    List<Object[]> countNichesInBoardBetween(@Param("boardId") Long boardId,
                                              @Param("startDate") LocalDateTime startDate,
                                              @Param("endDate") LocalDateTime endDate);

    // Count niches for completed designs across multiple boards
    @Query("SELECT n.name, COUNT(dd) FROM DesignDetail dd JOIN dd.card c JOIN dd.niches n " +
            "WHERE c.column.board.id IN :boardIds " +
            "AND dd.approvalDate >= :startDate AND dd.approvalDate < :endDate " +
            "GROUP BY n.name ORDER BY COUNT(dd) DESC")
    List<Object[]> countNichesInBoardsBetween(@Param("boardIds") List<Long> boardIds,
                                               @Param("startDate") LocalDateTime startDate,
                                               @Param("endDate") LocalDateTime endDate);
}
