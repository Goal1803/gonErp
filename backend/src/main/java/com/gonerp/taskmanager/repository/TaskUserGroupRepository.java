package com.gonerp.taskmanager.repository;

import com.gonerp.taskmanager.model.TaskUserGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TaskUserGroupRepository extends JpaRepository<TaskUserGroup, Long> {

    boolean existsByName(String name);

    boolean existsByNameAndIdNot(String name, Long id);
}
