package com.gonerp.taskmanager.repository;

import com.gonerp.taskmanager.model.DesignStaffRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DesignStaffRoleRepository extends JpaRepository<DesignStaffRole, Long> {

    boolean existsByName(String name);

    boolean existsByNameAndIdNot(String name, Long id);
}
