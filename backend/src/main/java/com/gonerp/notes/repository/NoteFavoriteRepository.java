package com.gonerp.notes.repository;

import com.gonerp.notes.model.NoteFavorite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NoteFavoriteRepository extends JpaRepository<NoteFavorite, Long> {

    @Query("SELECT f FROM NoteFavorite f JOIN FETCH f.note n JOIN FETCH n.owner "
            + "WHERE f.user.id = :userId AND n.archived = false ORDER BY f.position ASC")
    List<NoteFavorite> findByUserId(@Param("userId") Long userId);

    @Query("SELECT f.note.id FROM NoteFavorite f WHERE f.user.id = :userId")
    List<Long> findNoteIdsByUserId(@Param("userId") Long userId);

    Optional<NoteFavorite> findByNoteIdAndUserId(Long noteId, Long userId);

    void deleteByNoteId(Long noteId);
}
