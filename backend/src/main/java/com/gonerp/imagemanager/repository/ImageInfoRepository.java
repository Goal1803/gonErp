package com.gonerp.imagemanager.repository;

import com.gonerp.imagemanager.model.ImageInfo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ImageInfoRepository extends JpaRepository<ImageInfo, Long> {

    @Query("""
        SELECT i FROM ImageInfo i
        WHERE (:search IS NULL OR :search = ''
              OR LOWER(i.name) LIKE LOWER(CONCAT('%', :search, '%')))
        """)
    Page<ImageInfo> findAllWithSearch(@Param("search") String search, Pageable pageable);
}
