package com.gonerp.taskmanager.repository;

import com.gonerp.taskmanager.model.CardComment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CardCommentRepository extends JpaRepository<CardComment, Long> {

    // Batch comment counts: returns rows of [cardId, count] for the given cards.
    // Used to build card summaries without loading each card's full comment list.
    @Query("SELECT cc.card.id, COUNT(cc) FROM CardComment cc WHERE cc.card.id IN :cardIds GROUP BY cc.card.id")
    List<Object[]> countByCardIds(@Param("cardIds") List<Long> cardIds);
}
