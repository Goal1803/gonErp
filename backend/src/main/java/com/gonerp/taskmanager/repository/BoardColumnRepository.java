package com.gonerp.taskmanager.repository;

import com.gonerp.taskmanager.model.BoardColumn;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BoardColumnRepository extends JpaRepository<BoardColumn, Long> {

    List<BoardColumn> findByBoardIdOrderByPositionAsc(Long boardId);

    int countByBoardId(Long boardId);
}
