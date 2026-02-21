package com.gonerp.organization.repository;

import com.gonerp.organization.model.StaffRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StaffRoleRepository extends JpaRepository<StaffRole, Long> {

    List<StaffRole> findByOrgTypeId(Long orgTypeId);

    List<StaffRole> findByOrganizationId(Long organizationId);
}
