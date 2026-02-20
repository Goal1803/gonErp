package com.gonerp.taskmanager.repository;

import com.gonerp.taskmanager.model.Occasion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OccasionRepository extends JpaRepository<Occasion, Long> {
    boolean existsByName(String name);
}
