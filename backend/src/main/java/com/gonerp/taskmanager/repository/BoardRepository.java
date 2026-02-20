package com.gonerp.taskmanager.repository;

import com.gonerp.taskmanager.model.Board;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BoardRepository extends JpaRepository<Board, Long> {

    @Query("SELECT DISTINCT b FROM Board b JOIN FETCH b.owner WHERE b.active = true AND (b.owner.id = :userId OR EXISTS (SELECT m FROM BoardMember m WHERE m.board = b AND m.user.id = :userId))")
    List<Board> findAllVisibleToUser(@Param("userId") Long userId);

    @Query("SELECT DISTINCT b FROM Board b JOIN FETCH b.owner WHERE b.active = true")
    List<Board> findAllWithOwner();
}
