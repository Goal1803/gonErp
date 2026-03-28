package com.gonerp.ecommerce.repository;

import com.gonerp.ecommerce.model.EcomOrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EcomOrderItemRepository extends JpaRepository<EcomOrderItem, Long> {

    List<EcomOrderItem> findByOrderId(Long orderId);
}
