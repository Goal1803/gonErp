package com.gonerp.taskmanager.repository;

import com.gonerp.taskmanager.model.DesignUserGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DesignUserGroupRepository extends JpaRepository<DesignUserGroup, Long> {

    boolean existsByName(String name);

    boolean existsByNameAndIdNot(String name, Long id);
}
