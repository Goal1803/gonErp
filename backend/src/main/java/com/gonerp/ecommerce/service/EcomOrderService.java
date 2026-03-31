package com.gonerp.ecommerce.service;

import com.gonerp.ecommerce.dto.EcomOrderRequest;
import com.gonerp.ecommerce.dto.EcomOrderResponse;
import com.gonerp.ecommerce.model.EcomOrder;
import com.gonerp.ecommerce.model.enums.OrderStatus;
import com.gonerp.ecommerce.model.enums.TrackingStatus;
import com.gonerp.ecommerce.repository.EcomOrderRepository;
import com.gonerp.ecommerce.repository.EcomSupplierRepository;
import com.gonerp.organization.model.Organization;
import com.gonerp.taskmanager.model.Board;
import com.gonerp.taskmanager.model.BoardColumn;
import com.gonerp.taskmanager.model.enums.BoardType;
import com.gonerp.taskmanager.repository.BoardColumnRepository;
import com.gonerp.taskmanager.repository.BoardRepository;
import com.gonerp.usermanager.model.User;
import com.gonerp.usermanager.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class EcomOrderService {

    private final EcomOrderRepository ecomOrderRepository;
    private final EcomSupplierRepository ecomSupplierRepository;
    private final EcomAccessService ecomAccessService;
    private final EcomOrderImportHelper importHelper;
    private final BoardRepository boardRepository;
    private final BoardColumnRepository boardColumnRepository;
    private final UserRepository userRepository;

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
        if (request.getShippingAgent() != null) {
            order.setShippingAgent(request.getShippingAgent());
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
        if (request.getSupplierId() != null) {
            if (request.getSupplierId() == 0) {
                order.setSupplier(null);
            } else {
                order.setSupplier(ecomSupplierRepository.findById(request.getSupplierId())
                        .orElseThrow(() -> new EntityNotFoundException("Supplier not found")));
            }
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
        // Use earningAfterPlatformFee if available (from statement matching), otherwise orderNet
        BigDecimal base = order.getEarningAfterPlatformFee();
        if (base == null) base = order.getOrderNet();
        if (base == null) return;

        BigDecimal profit = base;
        if (order.getFulfillmentCost() != null) {
            profit = profit.subtract(order.getFulfillmentCost());
        }
        if (order.getOtherCost() != null) {
            profit = profit.subtract(order.getOtherCost());
        }
        order.setGrossProfit(profit);
    }

    public Map<String, Object> syncOrdersToBoard(List<Long> orderIds, Long boardId) {
        ecomAccessService.requireEcommerceAccess();

        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new EntityNotFoundException("Board not found: " + boardId));
        if (board.getBoardType() != BoardType.POD_ORDER) {
            throw new IllegalArgumentException("Board must be a POD_ORDER board");
        }

        List<BoardColumn> columns = boardColumnRepository.findByBoardIdOrderByPositionAsc(boardId);
        BoardColumn draftColumn = columns.stream()
                .filter(c -> "Draft".equalsIgnoreCase(c.getTitle()))
                .findFirst()
                .orElse(columns.isEmpty() ? null : columns.get(0));

        if (draftColumn == null) {
            throw new IllegalStateException("Board has no columns");
        }

        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findByUserName(username).orElse(null);

        int synced = 0;
        int skipped = 0;
        List<String> errors = new ArrayList<>();

        for (Long orderId : orderIds) {
            try {
                EcomOrder order = ecomOrderRepository.findById(orderId).orElse(null);
                if (order == null) { errors.add("Order " + orderId + " not found"); continue; }
                if (order.getCard() != null) { skipped++; continue; } // already synced
                if (!importHelper.hasRealItems(order)) { skipped++; continue; } // no real items
                importHelper.createCardForOrder(order, draftColumn, currentUser);
                synced++;
            } catch (Exception e) {
                log.warn("Failed to sync order {}: {}", orderId, e.getMessage());
                errors.add("Order " + orderId + ": " + e.getMessage());
            }
        }

        return Map.of("synced", synced, "skipped", skipped, "errors", errors);
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
