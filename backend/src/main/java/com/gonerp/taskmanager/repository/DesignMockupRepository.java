package com.gonerp.taskmanager.repository;

import com.gonerp.taskmanager.model.DesignMockup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DesignMockupRepository extends JpaRepository<DesignMockup, Long> {

    @Modifying
    @Query("UPDATE DesignMockup m SET m.mainMockup = false WHERE m.designDetail.id = :designDetailId")
    void clearMainMockup(@Param("designDetailId") Long designDetailId);

    /** Lean projection for image search: only the bits we need to rank. */
    interface HashRow {
        Long getId();
        Long getImageHash();
        Long getDesignDetailId();
    }

    @Query("SELECT m.id AS id, m.imageHash AS imageHash, m.designDetail.id AS designDetailId " +
           "FROM DesignMockup m WHERE m.imageHash IS NOT NULL")
    List<HashRow> findAllHashed();

    List<DesignMockup> findByImageHashIsNull();

    List<DesignMockup> findByImageHashIsNull(org.springframework.data.domain.Pageable pageable);

    long countByImageHashIsNull();
}
