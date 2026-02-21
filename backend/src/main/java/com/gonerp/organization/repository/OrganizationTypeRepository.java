package com.gonerp.organization.repository;

import com.gonerp.organization.model.OrganizationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OrganizationTypeRepository extends JpaRepository<OrganizationType, Long> {

    Optional<OrganizationType> findByName(String name);

    boolean existsByName(String name);
}
