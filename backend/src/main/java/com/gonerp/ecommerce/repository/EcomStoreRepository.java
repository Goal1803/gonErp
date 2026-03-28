package com.gonerp.ecommerce.repository;

import com.gonerp.ecommerce.model.EcomStore;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EcomStoreRepository extends JpaRepository<EcomStore, Long> {

    List<EcomStore> findByOrganizationIdOrderByNameAsc(Long organizationId);

    List<EcomStore> findByOrganizationIdAndActiveOrderByNameAsc(Long organizationId, boolean active);
}
