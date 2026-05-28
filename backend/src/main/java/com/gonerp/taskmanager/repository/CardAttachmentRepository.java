package com.gonerp.taskmanager.repository;

import com.gonerp.taskmanager.model.CardAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CardAttachmentRepository extends JpaRepository<CardAttachment, Long> {

    // Batch attachment counts: returns rows of [cardId, count] for the given cards.
    @Query("SELECT ca.card.id, COUNT(ca) FROM CardAttachment ca WHERE ca.card.id IN :cardIds GROUP BY ca.card.id")
    List<Object[]> countByCardIds(@Param("cardIds") List<Long> cardIds);
}
