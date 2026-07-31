package com.gonerp.notes.repository;

import com.gonerp.notes.model.Note;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NoteRepository extends JpaRepository<Note, Long> {

    /**
     * Every note the user may open: owned by them, or sitting anywhere inside a
     * sub-tree that was shared with them (matched through the materialised path).
     */
    String VISIBLE = """
            (n.owner.id = :userId OR EXISTS (
                SELECT m FROM NoteMember m WHERE m.user.id = :userId
                  AND n.path LIKE CONCAT('%/', m.note.id, '/%')))
            """;

    @Query("SELECT n FROM Note n JOIN FETCH n.owner WHERE n.archived = false AND n.template = false "
            + "AND n.rowOf IS NULL AND (:orgId IS NULL OR n.organization.id = :orgId) AND " + VISIBLE
            + " ORDER BY n.position ASC, n.id ASC")
    List<Note> findTree(@Param("userId") Long userId, @Param("orgId") Long orgId);

    @Query("SELECT n FROM Note n JOIN FETCH n.owner WHERE n.archived = false AND n.template = true "
            + "AND (:orgId IS NULL OR n.organization.id = :orgId) AND " + VISIBLE + " ORDER BY n.title ASC")
    List<Note> findTemplates(@Param("userId") Long userId, @Param("orgId") Long orgId);

    /** Trash listing — only the notes the user actually deleted, not dragged-along children. */
    @Query("SELECT n FROM Note n JOIN FETCH n.owner WHERE n.archived = true AND n.archivedRoot = true "
            + "AND (:orgId IS NULL OR n.organization.id = :orgId) AND " + VISIBLE + " ORDER BY n.lastUpdatedAt DESC")
    List<Note> findTrash(@Param("userId") Long userId, @Param("orgId") Long orgId);

    @Query("SELECT n FROM Note n JOIN FETCH n.owner WHERE n.archived = false "
            + "AND (:orgId IS NULL OR n.organization.id = :orgId) AND " + VISIBLE + " AND ("
            + "LOWER(n.title) LIKE LOWER(CONCAT('%', :q, '%')) "
            + "OR LOWER(n.plainText) LIKE LOWER(CONCAT('%', :q, '%'))) "
            + "ORDER BY n.lastUpdatedAt DESC")
    List<Note> search(@Param("userId") Long userId, @Param("orgId") Long orgId,
                      @Param("q") String q, Pageable pageable);

    /** Mention picker: title-only match, so typing "@pro" stays fast and predictable. */
    @Query("SELECT n FROM Note n WHERE n.archived = false AND (:orgId IS NULL OR n.organization.id = :orgId) AND " + VISIBLE
            + " AND LOWER(n.title) LIKE LOWER(CONCAT('%', :q, '%')) ORDER BY n.lastUpdatedAt DESC")
    List<Note> searchTitles(@Param("userId") Long userId, @Param("orgId") Long orgId,
                            @Param("q") String q, Pageable pageable);

    @Query("SELECT n FROM Note n JOIN FETCH n.owner WHERE n.archived = false "
            + "AND (:orgId IS NULL OR n.organization.id = :orgId) AND " + VISIBLE + " ORDER BY n.lastUpdatedAt DESC")
    List<Note> findRecent(@Param("userId") Long userId, @Param("orgId") Long orgId, Pageable pageable);

    List<Note> findByParentIdOrderByPositionAsc(Long parentId);

    @Query("SELECT n FROM Note n WHERE n.parent IS NULL AND n.owner.id = :ownerId "
            + "AND n.archived = false AND n.template = false ORDER BY n.position ASC, n.id ASC")
    List<Note> findTopLevelByOwner(@Param("ownerId") Long ownerId);

    @Query("SELECT COALESCE(MAX(n.position), -1) FROM Note n WHERE "
            + "(:parentId IS NULL AND n.parent IS NULL OR n.parent.id = :parentId) AND n.owner.id = :ownerId")
    int maxPosition(@Param("parentId") Long parentId, @Param("ownerId") Long ownerId);

    /** The note itself plus everything nested under it. */
    @Query("SELECT n FROM Note n WHERE n.path LIKE CONCAT('%/', :id, '/%')")
    List<Note> findSubtree(@Param("id") Long id);

    @Query("SELECT n FROM Note n WHERE n.parent.id IN :parentIds ORDER BY n.position ASC")
    List<Note> findByParentIds(@Param("parentIds") List<Long> parentIds);
}
