package com.gonerp.organization.repository;

import com.gonerp.organization.model.UserGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserGroupRepository extends JpaRepository<UserGroup, Long> {

    List<UserGroup> findByOrgTypeId(Long orgTypeId);

    List<UserGroup> findByOrganizationId(Long organizationId);
}
