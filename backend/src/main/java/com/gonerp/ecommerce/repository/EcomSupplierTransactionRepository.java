package com.gonerp.ecommerce.repository;

import com.gonerp.ecommerce.model.EcomSupplierTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface EcomSupplierTransactionRepository extends JpaRepository<EcomSupplierTransaction, Long> {

    List<EcomSupplierTransaction> findBySupplierIdOrderByOrderDateDesc(Long supplierId);

    Optional<EcomSupplierTransaction> findBySupplierIdAndSupplierOrderId(Long supplierId, String supplierOrderId);

    @Query("SELECT t FROM EcomSupplierTransaction t WHERE t.supplier.id = :supplierId AND t.matched = false " +
            "AND t.status NOT IN ('Cancelled', 'Not paid yet')")
    List<EcomSupplierTransaction> findUnmatchedBySupplierId(@Param("supplierId") Long supplierId);
}
