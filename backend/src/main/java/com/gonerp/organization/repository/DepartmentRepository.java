package com.gonerp.organization.repository;

import com.gonerp.organization.model.Department;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DepartmentRepository extends JpaRepository<Department, Long> {

    List<Department> findByOrgTypeId(Long orgTypeId);

    List<Department> findByOrganizationId(Long organizationId);
}
