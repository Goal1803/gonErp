package com.gonerp.taskmanager.repository;

import com.gonerp.taskmanager.model.TaskStaffRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TaskStaffRoleRepository extends JpaRepository<TaskStaffRole, Long> {

    boolean existsByName(String name);

    boolean existsByNameAndIdNot(String name, Long id);
}
