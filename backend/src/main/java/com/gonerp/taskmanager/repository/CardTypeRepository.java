package com.gonerp.taskmanager.repository;

import com.gonerp.taskmanager.model.CardType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CardTypeRepository extends JpaRepository<CardType, Long> {

    List<CardType> findByBoardId(Long boardId);

    @Modifying
    @Query(value = "DELETE FROM tm_card_type_map WHERE type_id = :typeId", nativeQuery = true)
    void deleteTypeMappings(@Param("typeId") Long typeId);
}
