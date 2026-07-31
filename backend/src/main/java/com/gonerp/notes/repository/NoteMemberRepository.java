package com.gonerp.notes.repository;

import com.gonerp.notes.model.NoteMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NoteMemberRepository extends JpaRepository<NoteMember, Long> {

    @Query("SELECT m FROM NoteMember m JOIN FETCH m.user WHERE m.note.id = :noteId")
    List<NoteMember> findByNoteId(@Param("noteId") Long noteId);

    Optional<NoteMember> findByNoteIdAndUserId(Long noteId, Long userId);

    /** Shares that apply to a note, i.e. placed on it or on any of its ancestors. */
    @Query("SELECT m FROM NoteMember m JOIN FETCH m.user WHERE m.note.id IN :noteIds")
    List<NoteMember> findByNoteIds(@Param("noteIds") List<Long> noteIds);

    void deleteByNoteId(Long noteId);

    void deleteByNoteIdAndUserId(Long noteId, Long userId);
}
