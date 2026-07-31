package com.gonerp.notes.repository;

import com.gonerp.notes.model.NoteProperty;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotePropertyRepository extends JpaRepository<NoteProperty, Long> {

    List<NoteProperty> findByDatabaseIdOrderByPositionAsc(Long databaseId);

    void deleteByDatabaseId(Long databaseId);
}
