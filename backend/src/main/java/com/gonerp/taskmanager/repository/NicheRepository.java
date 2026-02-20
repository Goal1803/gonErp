package com.gonerp.taskmanager.repository;

import com.gonerp.taskmanager.model.Niche;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NicheRepository extends JpaRepository<Niche, Long> {
    boolean existsByName(String name);
}
