package com.gonerp.taskmanager.repository;

import com.gonerp.taskmanager.model.DesignFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DesignFileRepository extends JpaRepository<DesignFile, Long> {
}
