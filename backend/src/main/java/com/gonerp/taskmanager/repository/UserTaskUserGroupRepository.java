package com.gonerp.taskmanager.repository;

import com.gonerp.taskmanager.model.UserTaskUserGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserTaskUserGroupRepository extends JpaRepository<UserTaskUserGroup, Long> {

    List<UserTaskUserGroup> findByUserId(Long userId);

    boolean existsByUserIdAndTaskUserGroupId(Long userId, Long taskUserGroupId);

    void deleteByUserIdAndTaskUserGroupId(Long userId, Long taskUserGroupId);
}
