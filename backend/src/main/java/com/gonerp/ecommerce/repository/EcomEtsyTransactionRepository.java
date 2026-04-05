package com.gonerp.ecommerce.repository;

import com.gonerp.ecommerce.model.EcomEtsyTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface EcomEtsyTransactionRepository extends JpaRepository<EcomEtsyTransaction, Long> {

    List<EcomEtsyTransaction> findByStoreIdOrderByTxnDateDesc(Long storeId);

    List<EcomEtsyTransaction> findByStoreIdAndOrderIdRefNotNullOrderByTxnDateDesc(Long storeId);

    @Query("SELECT COUNT(t) > 0 FROM EcomEtsyTransaction t WHERE t.store.id = :storeId " +
            "AND t.txnDate = :txnDate AND t.type = :type AND t.title = :title " +
            "AND t.info = :info AND t.amount = :amount AND t.feesAndTaxes = :feesAndTaxes AND t.currency = :currency")
    boolean existsByCompositeKey(@Param("storeId") Long storeId, @Param("txnDate") LocalDate txnDate,
                                 @Param("type") String type, @Param("title") String title,
                                 @Param("info") String info, @Param("amount") BigDecimal amount,
                                 @Param("feesAndTaxes") BigDecimal feesAndTaxes, @Param("currency") String currency);

    List<EcomEtsyTransaction> findByStoreIdAndOrderIdRef(Long storeId, String orderIdRef);

    @Query("SELECT DISTINCT t.orderIdRef FROM EcomEtsyTransaction t " +
            "WHERE t.store.id = :storeId AND t.orderIdRef IS NOT NULL AND t.matched = false")
    List<String> findUnmatchedOrderIds(@Param("storeId") Long storeId);

    List<EcomEtsyTransaction> findByStoreIdAndTxnDateBetweenOrderByTxnDateDesc(
            Long storeId, LocalDate start, LocalDate end);

    @Query("SELECT t FROM EcomEtsyTransaction t WHERE t.store.id IN :storeIds " +
            "AND t.txnDate >= :start AND t.txnDate <= :end ORDER BY t.txnDate DESC")
    List<EcomEtsyTransaction> findByStoreIdsAndDateRange(@Param("storeIds") List<Long> storeIds,
            @Param("start") LocalDate start, @Param("end") LocalDate end);
}
