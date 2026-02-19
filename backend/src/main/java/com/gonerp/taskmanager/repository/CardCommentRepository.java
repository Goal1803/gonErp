package com.gonerp.taskmanager.repository;

import com.gonerp.taskmanager.model.CardComment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CardCommentRepository extends JpaRepository<CardComment, Long> {
}
