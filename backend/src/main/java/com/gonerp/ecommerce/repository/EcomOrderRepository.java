package com.gonerp.ecommerce.repository;

import com.gonerp.ecommerce.model.EcomOrder;
import com.gonerp.ecommerce.model.enums.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;

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
}
