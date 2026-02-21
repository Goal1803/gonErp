package com.gonerp.taskmanager.repository;

import com.gonerp.taskmanager.model.UserDesignStaffRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserDesignStaffRoleRepository extends JpaRepository<UserDesignStaffRole, Long> {

    List<UserDesignStaffRole> findByUserId(Long userId);

    boolean existsByUserIdAndDesignStaffRoleId(Long userId, Long designStaffRoleId);

    void deleteByUserIdAndDesignStaffRoleId(Long userId, Long designStaffRoleId);
}
