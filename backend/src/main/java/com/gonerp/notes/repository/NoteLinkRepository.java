package com.gonerp.notes.repository;

import com.gonerp.notes.model.NoteLink;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NoteLinkRepository extends JpaRepository<NoteLink, Long> {

    /** Backlinks: notes that mention this one. */
    @Query("SELECT l FROM NoteLink l JOIN FETCH l.source s JOIN FETCH s.owner "
            + "WHERE l.target.id = :noteId AND s.archived = false")
    List<NoteLink> findBySourceOf(@Param("noteId") Long noteId);

    @Query("SELECT l FROM NoteLink l JOIN FETCH l.target t WHERE l.source.id = :noteId AND t.archived = false")
    List<NoteLink> findOutgoing(@Param("noteId") Long noteId);

    void deleteBySourceId(Long sourceId);

    void deleteByTargetId(Long targetId);
}
