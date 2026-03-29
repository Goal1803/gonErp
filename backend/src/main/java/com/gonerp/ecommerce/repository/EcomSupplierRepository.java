package com.gonerp.ecommerce.repository;

import com.gonerp.ecommerce.model.EcomSupplier;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EcomSupplierRepository extends JpaRepository<EcomSupplier, Long> {
    List<EcomSupplier> findByOrganizationIdAndActiveTrueOrderByNameAsc(Long orgId);
    List<EcomSupplier> findByOrganizationIdOrderByNameAsc(Long orgId);
}
