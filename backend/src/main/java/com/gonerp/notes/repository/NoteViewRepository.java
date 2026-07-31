package com.gonerp.notes.repository;

import com.gonerp.notes.model.NoteView;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NoteViewRepository extends JpaRepository<NoteView, Long> {

    List<NoteView> findByDatabaseIdOrderByPositionAsc(Long databaseId);

    void deleteByDatabaseId(Long databaseId);
}
