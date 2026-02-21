package com.gonerp.organization.repository;

import com.gonerp.organization.model.UserStaffRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserStaffRoleRepository extends JpaRepository<UserStaffRole, Long> {

    List<UserStaffRole> findByUserId(Long userId);

    boolean existsByUserIdAndStaffRoleId(Long userId, Long staffRoleId);

    void deleteByUserIdAndStaffRoleId(Long userId, Long staffRoleId);
}
