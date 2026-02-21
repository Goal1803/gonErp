package com.gonerp.organization.repository;

import com.gonerp.organization.model.UserDepartment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserDepartmentRepository extends JpaRepository<UserDepartment, Long> {

    List<UserDepartment> findByUserId(Long userId);

    boolean existsByUserIdAndDepartmentId(Long userId, Long departmentId);

    void deleteByUserIdAndDepartmentId(Long userId, Long departmentId);
}
