package com.gonerp.ecommerce.service;

import com.gonerp.ecommerce.dto.EcomOrderRequest;
import com.gonerp.ecommerce.dto.EcomOrderResponse;
import com.gonerp.ecommerce.model.EcomOrder;
import com.gonerp.ecommerce.model.enums.OrderStatus;
import com.gonerp.ecommerce.model.enums.TrackingStatus;
import com.gonerp.ecommerce.repository.EcomOrderRepository;
import com.gonerp.organization.model.Organization;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class EcomOrderService {

    private final EcomOrderRepository ecomOrderRepository;
    private final EcomAccessService ecomAccessService;

    public List<EcomOrderResponse> findAll(Long storeId, String status,
                                            LocalDateTime startDate, LocalDateTime endDate) {
        ecomAccessService.requireEcommerceAccess();
        Organization org = ecomAccessService.resolveOrganization();

        List<EcomOrder> orders;

        if (storeId != null) {
            orders = ecomOrderRepository.findByStoreIdOrderByOrderDateDesc(storeId);
        } else if (status != null && !status.isBlank()) {
            OrderStatus orderStatus = OrderStatus.valueOf(status);
            orders = ecomOrderRepository.findByOrganizationIdAndStatusOrderByOrderDateDesc(org.getId(), orderStatus);
        } else if (startDate != null && endDate != null) {
            orders = ecomOrderRepository.findByOrganizationIdAndOrderDateBetweenOrderByOrderDateDesc(
                    org.getId(), startDate, endDate);
        } else {
            orders = ecomOrderRepository.findByOrganizationIdOrderByOrderDateDesc(org.getId());
        }

        return orders.stream()
                .map(EcomOrderResponse::from)
                .toList();
    }

    public EcomOrderResponse findById(Long id) {
        ecomAccessService.requireEcommerceAccess();
        EcomOrder order = getOrderInOrg(id);
        return EcomOrderResponse.from(order);
    }

    public EcomOrderResponse update(Long id, EcomOrderRequest request) {
        ecomAccessService.requireEcommerceAccess();
        EcomOrder order = getOrderInOrg(id);

        if (request.getStatus() != null) {
            order.setStatus(OrderStatus.valueOf(request.getStatus()));
        }
        if (request.getTrackingNumber() != null) {
            order.setTrackingNumber(request.getTrackingNumber());
        }
        if (request.getTrackingStatus() != null) {
            order.setTrackingStatus(TrackingStatus.valueOf(request.getTrackingStatus()));
        }
        if (request.getFulfillmentCost() != null) {
            order.setFulfillmentCost(request.getFulfillmentCost());
        }
        if (request.getOtherCost() != null) {
            order.setOtherCost(request.getOtherCost());
        }
        if (request.getNotes() != null) {
            order.setNotes(request.getNotes());
        }
        if (request.getRefunded() != null) {
            order.setRefunded(request.getRefunded());
        }

        // Auto-calculate gross profit whenever costs change
        calculateGrossProfit(order);

        return EcomOrderResponse.from(ecomOrderRepository.save(order));
    }

    public void delete(Long id) {
        ecomAccessService.requireEcommerceAccess();
        EcomOrder order = getOrderInOrg(id);
        ecomOrderRepository.delete(order);
    }

    /**
     * Calculate gross profit: orderNet - fulfillmentCost - otherCost
     */
    public void calculateGrossProfit(EcomOrder order) {
        BigDecimal net = order.getOrderNet();
        if (net == null) return;

        BigDecimal profit = net;
        if (order.getFulfillmentCost() != null) {
            profit = profit.subtract(order.getFulfillmentCost());
        }
        if (order.getOtherCost() != null) {
            profit = profit.subtract(order.getOtherCost());
        }
        order.setGrossProfit(profit);
    }

    private EcomOrder getOrderInOrg(Long id) {
        Organization org = ecomAccessService.resolveOrganization();
        EcomOrder order = ecomOrderRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Order not found: " + id));
        if (!order.getOrganization().getId().equals(org.getId())) {
            throw new SecurityException("Access denied");
        }
        return order;
    }
}
