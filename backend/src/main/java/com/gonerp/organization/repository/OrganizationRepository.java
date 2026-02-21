package com.gonerp.organization.repository;

import com.gonerp.organization.model.Organization;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OrganizationRepository extends JpaRepository<Organization, Long> {

    Optional<Organization> findBySlug(String slug);

    boolean existsByName(String name);

    boolean existsBySlug(String slug);

    long countByOrgTypeId(Long orgTypeId);
}
