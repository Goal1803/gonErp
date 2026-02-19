package com.gonerp.taskmanager.repository;

import com.gonerp.taskmanager.model.BoardMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BoardMemberRepository extends JpaRepository<BoardMember, Long> {

    Optional<BoardMember> findByBoardIdAndUserId(Long boardId, Long userId);

    boolean existsByBoardIdAndUserId(Long boardId, Long userId);

    void deleteByBoardIdAndUserId(Long boardId, Long userId);
}
