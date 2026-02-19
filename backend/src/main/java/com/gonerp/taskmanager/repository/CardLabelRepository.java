package com.gonerp.taskmanager.repository;

import com.gonerp.taskmanager.model.CardLabel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CardLabelRepository extends JpaRepository<CardLabel, Long> {

    List<CardLabel> findByBoardId(Long boardId);

    @Modifying
    @Query(value = "DELETE FROM tm_card_label_map WHERE label_id = :labelId", nativeQuery = true)
    void deleteLabelMappings(@Param("labelId") Long labelId);
}
