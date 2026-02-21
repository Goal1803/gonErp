package com.gonerp.taskmanager.repository;

import com.gonerp.taskmanager.model.UserTaskStaffRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserTaskStaffRoleRepository extends JpaRepository<UserTaskStaffRole, Long> {

    List<UserTaskStaffRole> findByUserId(Long userId);

    boolean existsByUserIdAndTaskStaffRoleId(Long userId, Long taskStaffRoleId);

    void deleteByUserIdAndTaskStaffRoleId(Long userId, Long taskStaffRoleId);
}
