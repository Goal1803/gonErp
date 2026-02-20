package com.gonerp.taskmanager.repository;

import com.gonerp.taskmanager.model.DesignMockup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface DesignMockupRepository extends JpaRepository<DesignMockup, Long> {

    @Modifying
    @Query("UPDATE DesignMockup m SET m.mainMockup = false WHERE m.designDetail.id = :designDetailId")
    void clearMainMockup(@Param("designDetailId") Long designDetailId);
}
