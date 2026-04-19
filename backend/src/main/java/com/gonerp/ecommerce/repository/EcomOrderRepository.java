package com.gonerp.ecommerce.repository;

import com.gonerp.ecommerce.model.EcomOrder;
import com.gonerp.ecommerce.model.enums.OrderStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface EcomOrderRepository extends JpaRepository<EcomOrder, Long> {

    List<EcomOrder> findByOrganizationIdOrderByOrderDateDesc(Long orgId);

    List<EcomOrder> findByStoreIdOrderByOrderDateDesc(Long storeId);

    List<EcomOrder> findByOrganizationIdAndStatusOrderByOrderDateDesc(Long orgId, OrderStatus status);

    Optional<EcomOrder> findByPlatformOrderIdAndStoreId(String platformOrderId, Long storeId);

    long countByStoreId(Long storeId);

    Optional<EcomOrder> findByCardId(Long cardId);

    List<EcomOrder> findByOrganizationIdAndOrderDateBetweenOrderByOrderDateDesc(
            Long orgId, LocalDateTime start, LocalDateTime end);

    List<EcomOrder> findByStoreIdAndOrderDateBetweenOrderByOrderDateDesc(
            Long storeId, LocalDateTime start, LocalDateTime end);

    @Query("SELECT o FROM EcomOrder o WHERE o.organization.id = :orgId AND (" +
            "LOWER(o.platformOrderId) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
            "LOWER(o.customerName) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
            "LOWER(o.shipStreet1) LIKE LOWER(CONCAT('%', :q, '%'))) " +
            "ORDER BY o.orderDate DESC")
    List<EcomOrder> searchByOrganization(@Param("orgId") Long orgId,
                                          @Param("q") String q,
                                          Pageable pageable);

    // Find the latest order (by createdAt DESC) within the same board whose items share
    // any SKU with the given set, excluding the current order. Used as a fallback for
    // bulk auto-assign on POD_ORDER boards when no matching design is found.
    @Query("SELECT DISTINCT o FROM EcomOrder o JOIN o.items i " +
            "WHERE i.sku IN :skus " +
            "AND o.id <> :excludeOrderId " +
            "AND o.card.column.board.id = :boardId " +
            "ORDER BY o.createdAt DESC")
    List<EcomOrder> findLatestByItemSkuOverlap(@Param("skus") List<String> skus,
                                                @Param("excludeOrderId") Long excludeOrderId,
                                                @Param("boardId") Long boardId,
                                                Pageable pageable);
}
