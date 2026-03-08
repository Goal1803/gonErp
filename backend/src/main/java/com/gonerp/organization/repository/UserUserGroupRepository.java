package com.gonerp.organization.repository;

import com.gonerp.organization.model.UserUserGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserUserGroupRepository extends JpaRepository<UserUserGroup, Long> {

    List<UserUserGroup> findByUserId(Long userId);

    List<UserUserGroup> findByUserGroupId(Long userGroupId);

    boolean existsByUserIdAndUserGroupId(Long userId, Long userGroupId);

    void deleteByUserIdAndUserGroupId(Long userId, Long userGroupId);
}
