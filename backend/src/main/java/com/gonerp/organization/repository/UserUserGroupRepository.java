package com.gonerp.organization.repository;

import com.gonerp.organization.model.UserUserGroup;
import com.gonerp.organization.model.enums.GroupRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserUserGroupRepository extends JpaRepository<UserUserGroup, Long> {

    List<UserUserGroup> findByUserId(Long userId);

    List<UserUserGroup> findByUserGroupId(Long userGroupId);

    Optional<UserUserGroup> findByUserIdAndUserGroupId(Long userId, Long userGroupId);

    Optional<UserUserGroup> findByUserGroupIdAndGroupRole(Long userGroupId, GroupRole groupRole);

    boolean existsByUserIdAndUserGroupId(Long userId, Long userGroupId);

    void deleteByUserIdAndUserGroupId(Long userId, Long userGroupId);
}
