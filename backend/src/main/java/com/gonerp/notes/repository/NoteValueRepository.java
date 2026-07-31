package com.gonerp.notes.repository;

import com.gonerp.notes.model.NoteValue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NoteValueRepository extends JpaRepository<NoteValue, Long> {

    /** Every cell of a database, fetched in one go and grouped in memory. */
    @Query("SELECT v FROM NoteValue v JOIN FETCH v.property p WHERE p.database.id = :databaseId")
    List<NoteValue> findByDatabaseId(@Param("databaseId") Long databaseId);

    List<NoteValue> findByRowId(Long rowId);

    Optional<NoteValue> findByRowIdAndPropertyId(Long rowId, Long propertyId);

    void deleteByRowId(Long rowId);

    void deleteByPropertyId(Long propertyId);
}
