package com.gonerp.taskmanager.repository;

import com.gonerp.taskmanager.model.CommentReaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CommentReactionRepository extends JpaRepository<CommentReaction, Long> {

    List<CommentReaction> findByCommentId(Long commentId);

    Optional<CommentReaction> findByCommentIdAndUserId(Long commentId, Long userId);

    void deleteByCommentIdAndUserId(Long commentId, Long userId);
}
