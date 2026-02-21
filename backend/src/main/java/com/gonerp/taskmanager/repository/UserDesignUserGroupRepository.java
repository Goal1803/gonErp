package com.gonerp.taskmanager.repository;

import com.gonerp.taskmanager.model.UserDesignUserGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserDesignUserGroupRepository extends JpaRepository<UserDesignUserGroup, Long> {

    List<UserDesignUserGroup> findByUserId(Long userId);

    boolean existsByUserIdAndDesignUserGroupId(Long userId, Long designUserGroupId);

    void deleteByUserIdAndDesignUserGroupId(Long userId, Long designUserGroupId);
}
