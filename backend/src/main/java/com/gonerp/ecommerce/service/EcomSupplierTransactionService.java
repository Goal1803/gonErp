package com.gonerp.ecommerce.service;

import com.gonerp.ecommerce.dto.EcomSupplierTransactionResponse;
import com.gonerp.ecommerce.model.EcomOrder;
import com.gonerp.ecommerce.model.EcomSupplier;
import com.gonerp.ecommerce.model.EcomSupplierTransaction;
import com.gonerp.ecommerce.model.enums.OrderStatus;
import com.gonerp.ecommerce.parser.SupplierFileParser;
import com.gonerp.ecommerce.repository.EcomOrderRepository;
import com.gonerp.ecommerce.repository.EcomSupplierRepository;
import com.gonerp.ecommerce.repository.EcomSupplierTransactionRepository;
import com.gonerp.finance.repository.FinanceCurrencyRateRepository;
import com.gonerp.organization.model.Organization;
import com.gonerp.taskmanager.model.Board;
import com.gonerp.taskmanager.model.BoardColumn;
import com.gonerp.taskmanager.model.Card;
import com.gonerp.taskmanager.model.enums.BoardType;
import com.gonerp.taskmanager.repository.BoardColumnRepository;
import com.gonerp.taskmanager.repository.CardRepository;
import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class EcomSupplierTransactionService {

    private final EcomSupplierTransactionRepository txnRepository;
    private final EcomSupplierRepository supplierRepository;
    private final EcomOrderRepository orderRepository;
    private final FinanceCurrencyRateRepository rateRepository;
    private final EcomAccessService accessService;
    private final List<SupplierFileParser> parsers;
    private final BoardColumnRepository boardColumnRepository;
    private final CardRepository cardRepository;

    private Map<String, SupplierFileParser> parserMap;

    @PostConstruct
    void initParsers() {
        parserMap = new HashMap<>();
        for (SupplierFileParser parser : parsers) {
            parserMap.put(parser.getSupplierName().toLowerCase(), parser);
        }
        log.info("Registered {} supplier file parsers: {}", parserMap.size(), parserMap.keySet());
    }

    /**
     * Upload supplier transaction file. Parser is selected by supplier name.
     */
    public Map<String, Object> uploadTransactions(Long supplierId, MultipartFile file) {
        accessService.requireEcommerceAccess();
        EcomSupplier supplier = supplierRepository.findById(supplierId)
                .orElseThrow(() -> new EntityNotFoundException("Supplier not found"));

        SupplierFileParser parser = parserMap.get(supplier.getName().toLowerCase());
        if (parser == null) {
            throw new IllegalArgumentException("No file parser available for supplier: " + supplier.getName()
                    + ". Supported suppliers: " + String.join(", ", parserMap.keySet()));
        }

        int inserted = 0, updated = 0, skipped = 0, errors = 0;

        try {
            List<EcomSupplierTransaction> parsed = parser.parse(supplier, file);

            for (EcomSupplierTransaction txn : parsed) {
                try {
                    Optional<EcomSupplierTransaction> existingOpt =
                            txnRepository.findBySupplierIdAndSupplierOrderId(supplierId, txn.getSupplierOrderId());

                    if (existingOpt.isPresent()) {
                        EcomSupplierTransaction existing = existingOpt.get();
                        boolean changed = false;
                        boolean trackingChanged = false;
                        if (txn.getTrackingId() != null && !txn.getTrackingId().equals(existing.getTrackingId())) {
                            existing.setTrackingId(txn.getTrackingId());
                            changed = true;
                            trackingChanged = true;
                        }
                        if (txn.getStatus() != null && !txn.getStatus().equals(existing.getStatus())) {
                            existing.setStatus(txn.getStatus());
                            changed = true;
                        }
                        if (txn.getExternalNumber() != null
                                && (existing.getExternalNumber() == null || existing.getExternalNumber().isBlank())) {
                            existing.setExternalNumber(txn.getExternalNumber());
                            changed = true;
                        }
                        if (changed) {
                            txnRepository.save(existing);
                            updated++;
                        } else {
                            skipped++;
                        }
                        // If tracking arrived in a later import and this txn is already matched,
                        // propagate it to the linked order and advance status → moves the board card
                        // into "Track Generated" automatically.
                        if (trackingChanged && existing.isMatched() && existing.getMatchedOrder() != null) {
                            propagateTrackingToOrder(existing);
                        }
                    } else {
                        txnRepository.save(txn);
                        inserted++;
                    }
                } catch (Exception e) {
                    log.warn("Failed to process supplier row: {}", e.getMessage());
                    errors++;
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse supplier file: " + e.getMessage(), e);
        }

        return Map.of("inserted", inserted, "updated", updated, "skipped", skipped, "errors", errors);
    }

    /**
     * Match unmatched supplier transactions to orders by customer name + street address.
     */
    public Map<String, Object> matchToOrders(Long supplierId, BigDecimal manualExchangeRate) {
        accessService.requireEcommerceAccess();
        Organization org = accessService.resolveOrganization();

        List<EcomSupplierTransaction> unmatched = txnRepository.findUnmatchedBySupplierId(supplierId);
        List<EcomOrder> allOrders = orderRepository.findByOrganizationIdOrderByOrderDateDesc(org.getId());

        // Build lookup: normalized(name + street) -> list of orders (to detect ambiguous)
        Map<String, List<EcomOrder>> orderLookup = new LinkedHashMap<>();
        // Secondary lookup by platformOrderId, used when the supplier txn carries an
        // explicit external number (e.g. Merchize "External number" = buyer-facing Etsy id).
        Map<String, List<EcomOrder>> externalLookup = new LinkedHashMap<>();
        for (EcomOrder order : allOrders) {
            String fullStreet = combineStreet(order.getShipStreet1(), order.getShipStreet2());
            String key = normalizeForMatch(order.getCustomerName(), fullStreet);
            if (key != null) {
                orderLookup.computeIfAbsent(key, k -> new ArrayList<>()).add(order);
            }
            String pid = order.getPlatformOrderId();
            if (pid != null && !pid.isBlank()) {
                externalLookup.computeIfAbsent(pid.trim(), k -> new ArrayList<>()).add(order);
            }
        }

        int matched = 0, noMatch = 0, alreadyHasCost = 0, ambiguous = 0;
        List<String> matchErrors = new ArrayList<>();
        List<Map<String, Object>> ambiguousMatches = new ArrayList<>();

        for (EcomSupplierTransaction txn : unmatched) {
            List<EcomOrder> candidates = null;

            // 1. Try external number first (exact match on platformOrderId)
            String externalNum = txn.getExternalNumber();
            if (externalNum != null && !externalNum.isBlank()) {
                List<EcomOrder> byExternal = externalLookup.get(externalNum.trim());
                if (byExternal != null && !byExternal.isEmpty()) {
                    candidates = byExternal;
                }
            }

            // 2. Fall back to customer name + street address
            if (candidates == null) {
                String key = normalizeForMatch(txn.getFullName(), txn.getStreetAddress());
                if (key == null) { noMatch++; continue; }
                candidates = orderLookup.get(key);
                if (candidates == null || candidates.isEmpty()) { noMatch++; continue; }
            }

            if (candidates.size() > 1) {
                // Multiple orders match — flag as ambiguous for manual resolution
                ambiguous++;
                List<Map<String, Object>> orderSummaries = candidates.stream().map(o -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("orderId", o.getId());
                    m.put("platformOrderId", o.getPlatformOrderId());
                    m.put("orderDate", o.getOrderDate());
                    m.put("orderTotal", o.getOrderTotal());
                    m.put("orderNet", o.getOrderNet());
                    m.put("currency", o.getCurrency());
                    m.put("customerName", o.getCustomerName());
                    m.put("shipStreet1", combineStreet(o.getShipStreet1(), o.getShipStreet2()));
                    m.put("shipCity", o.getShipCity());
                    m.put("shipState", o.getShipState());
                    m.put("shipZipcode", o.getShipZipcode());
                    m.put("shipCountry", o.getShipCountry());
                    m.put("sku", o.getSku());
                    m.put("numberOfItems", o.getNumberOfItems());
                    m.put("status", o.getStatus() != null ? o.getStatus().name() : null);
                    m.put("storeName", o.getStore() != null ? o.getStore().getName() : null);
                    m.put("synced", o.getCard() != null);
                    // Include item details
                    if (o.getItems() != null) {
                        m.put("items", o.getItems().stream().map(item -> {
                            Map<String, Object> im = new LinkedHashMap<>();
                            im.put("productName", item.getProductName());
                            im.put("sku", item.getSku());
                            im.put("quantity", item.getQuantity());
                            im.put("variations", item.getVariations());
                            im.put("itemPrice", item.getItemPrice());
                            return im;
                        }).toList());
                    }
                    return m;
                }).toList();
                Map<String, Object> ambEntry = new LinkedHashMap<>();
                ambEntry.put("transactionId", txn.getId());
                ambEntry.put("supplierOrderId", txn.getSupplierOrderId());
                ambEntry.put("fullName", txn.getFullName());
                ambEntry.put("streetAddress", txn.getStreetAddress());
                ambEntry.put("city", txn.getCity());
                ambEntry.put("stateRegion", txn.getStateRegion());
                ambEntry.put("postalCode", txn.getPostalCode());
                ambEntry.put("trackingId", txn.getTrackingId());
                ambEntry.put("amount", txn.getAmount());
                ambEntry.put("candidates", orderSummaries);
                ambiguousMatches.add(ambEntry);
                continue;
            }

            EcomOrder order = candidates.get(0);

            try {
                BigDecimal fulfillmentCost = txn.getAmount();
                if (fulfillmentCost != null) {
                    String txnCurrency = txn.getCurrency() != null ? txn.getCurrency() : "USD";
                    String storeCurrency = order.getCurrency();
                    if (storeCurrency != null && !storeCurrency.equalsIgnoreCase(txnCurrency)) {
                        BigDecimal rate = null;
                        try {
                            var rateOpt = rateRepository.findRateForDate(
                                    org.getId(), txnCurrency.toUpperCase(), storeCurrency.toUpperCase(), LocalDate.now());
                            if (rateOpt.isPresent()) rate = rateOpt.get().getRate();
                        } catch (Exception ignored) {}
                        if (rate == null) rate = manualExchangeRate;
                        if (rate == null) {
                            matchErrors.add(txn.getSupplierOrderId() + ": No exchange rate " + txnCurrency + "->" + storeCurrency);
                            continue;
                        }
                        fulfillmentCost = fulfillmentCost.multiply(rate).setScale(2, RoundingMode.HALF_UP);
                    }
                }

                if (order.getFulfillmentCost() == null || order.getFulfillmentCost().compareTo(BigDecimal.ZERO) == 0) {
                    order.setFulfillmentCost(fulfillmentCost);
                } else {
                    alreadyHasCost++;
                }

                if (txn.getTrackingId() != null && !txn.getTrackingId().isBlank()) {
                    if (order.getTrackingNumber() == null || order.getTrackingNumber().isBlank()) {
                        order.setTrackingNumber(txn.getTrackingId());
                    }
                }
                if (txn.getShipMethod() != null && !txn.getShipMethod().isBlank()) {
                    if (order.getShippingAgent() == null || order.getShippingAgent().isBlank()) {
                        order.setShippingAgent(txn.getShipMethod());
                    }
                }
                if (order.getSupplier() == null) {
                    order.setSupplier(supplierRepository.findById(supplierId).orElse(null));
                }
                order.setSupplierTransactionId(txn.getSupplierOrderId());

                recalculateGrossProfit(order);
                advanceOrderStatus(order);
                orderRepository.save(order);

                txn.setMatchedOrder(order);
                txn.setMatched(true);
                txnRepository.save(txn);
                matched++;
            } catch (Exception e) {
                matchErrors.add(txn.getSupplierOrderId() + ": " + e.getMessage());
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("matched", matched);
        result.put("noMatch", noMatch);
        result.put("alreadyHasCost", alreadyHasCost);
        result.put("ambiguous", ambiguous);
        result.put("ambiguousMatches", ambiguousMatches);
        result.put("errors", matchErrors);
        result.put("totalUnmatched", unmatched.size());
        return result;
    }

    /**
     * Manually match a specific supplier transaction to a specific order.
     */
    public void manualMatch(Long supplierId, Long transactionId, Long orderId, BigDecimal manualExchangeRate) {
        accessService.requireEcommerceAccess();
        Organization org = accessService.resolveOrganization();

        EcomSupplierTransaction txn = txnRepository.findById(transactionId)
                .orElseThrow(() -> new EntityNotFoundException("Transaction not found"));
        if (!txn.getSupplier().getId().equals(supplierId)) {
            throw new SecurityException("Transaction does not belong to this supplier");
        }

        EcomOrder order = orderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Order not found"));

        BigDecimal fulfillmentCost = txn.getAmount();
        if (fulfillmentCost != null) {
            String txnCurrency = txn.getCurrency() != null ? txn.getCurrency() : "USD";
            String storeCurrency = order.getCurrency();
            if (storeCurrency != null && !storeCurrency.equalsIgnoreCase(txnCurrency)) {
                BigDecimal rate = null;
                try {
                    var rateOpt = rateRepository.findRateForDate(
                            org.getId(), txnCurrency.toUpperCase(), storeCurrency.toUpperCase(), LocalDate.now());
                    if (rateOpt.isPresent()) rate = rateOpt.get().getRate();
                } catch (Exception ignored) {}
                if (rate == null) rate = manualExchangeRate;
                if (rate == null) throw new IllegalStateException("No exchange rate available");
                fulfillmentCost = fulfillmentCost.multiply(rate).setScale(2, java.math.RoundingMode.HALF_UP);
            }
        }

        if (order.getFulfillmentCost() == null || order.getFulfillmentCost().compareTo(BigDecimal.ZERO) == 0) {
            order.setFulfillmentCost(fulfillmentCost);
        }
        if (txn.getTrackingId() != null && !txn.getTrackingId().isBlank() &&
                (order.getTrackingNumber() == null || order.getTrackingNumber().isBlank())) {
            order.setTrackingNumber(txn.getTrackingId());
        }
        if (txn.getShipMethod() != null && !txn.getShipMethod().isBlank() &&
                (order.getShippingAgent() == null || order.getShippingAgent().isBlank())) {
            order.setShippingAgent(txn.getShipMethod());
        }
        if (order.getSupplier() == null) {
            order.setSupplier(txn.getSupplier());
        }
        order.setSupplierTransactionId(txn.getSupplierOrderId());
        recalculateGrossProfit(order);
        advanceOrderStatus(order);
        orderRepository.save(order);

        txn.setMatchedOrder(order);
        txn.setMatched(true);
        txnRepository.save(txn);
    }

    public List<EcomSupplierTransactionResponse> findAll(Long supplierId) {
        accessService.requireEcommerceAccess();
        return txnRepository.findBySupplierIdOrderByOrderDateDesc(supplierId).stream()
                .map(EcomSupplierTransactionResponse::from).toList();
    }

    private void recalculateGrossProfit(EcomOrder order) {
        BigDecimal base = order.getEarningAfterPlatformFee();
        if (base == null) base = order.getOrderNet();
        if (base == null) return;
        BigDecimal profit = base;
        if (order.getFulfillmentCost() != null) profit = profit.subtract(order.getFulfillmentCost());
        if (order.getOtherCost() != null) profit = profit.subtract(order.getOtherCost());
        order.setGrossProfit(profit);
    }

    private String combineStreet(String street1, String street2) {
        String s1 = street1 != null ? street1.trim() : "";
        String s2 = street2 != null ? street2.trim() : "";
        if (s1.isEmpty()) return s2.isEmpty() ? null : s2;
        if (s2.isEmpty()) return s1;
        return s1 + " " + s2;
    }

    /**
     * Push a newly-arrived tracking id onto the already-matched order and advance
     * its status → moves the linked board card into the Track Generated column.
     * Fill-only: does not overwrite an existing tracking number on the order.
     */
    private void propagateTrackingToOrder(EcomSupplierTransaction txn) {
        EcomOrder order = txn.getMatchedOrder();
        if (order == null) return;
        String newTracking = txn.getTrackingId();
        if (newTracking == null || newTracking.isBlank()) return;

        boolean touched = false;
        if (order.getTrackingNumber() == null || order.getTrackingNumber().isBlank()) {
            order.setTrackingNumber(newTracking);
            touched = true;
        }
        if (txn.getShipMethod() != null && !txn.getShipMethod().isBlank()
                && (order.getShippingAgent() == null || order.getShippingAgent().isBlank())) {
            order.setShippingAgent(txn.getShipMethod());
            touched = true;
        }
        if (!touched) return;

        advanceOrderStatus(order);
        orderRepository.save(order);
    }

    /**
     * Advance order status and move synced card based on fulfillment state:
     * - If order is in supplier file (matched) and status is before FULFILLED → move to FULFILLED
     * - If order has tracking number and status is before TRACK_GENERATED → move to TRACK_GENERATED
     */
    private void advanceOrderStatus(EcomOrder order) {
        OrderStatus current = order.getStatus();
        if (current == null) return;

        OrderStatus target = null;

        // Has tracking → should be at least TRACK_GENERATED
        if (order.getTrackingNumber() != null && !order.getTrackingNumber().isBlank()) {
            if (current.ordinal() < OrderStatus.TRACK_GENERATED.ordinal()) {
                target = OrderStatus.TRACK_GENERATED;
            }
        }
        // Matched to supplier (fulfilled) → should be at least FULFILLED
        else if (current.ordinal() < OrderStatus.FULFILLED.ordinal()) {
            target = OrderStatus.FULFILLED;
        }

        if (target == null) return;

        order.setStatus(target);

        // Move synced card to matching column
        Card card = order.getCard();
        if (card != null) {
            Board board = card.getColumn().getBoard();
            if (board.getBoardType() == BoardType.POD_ORDER) {
                String targetColumnTitle = mapStatusToColumnTitle(target);
                if (targetColumnTitle != null) {
                    List<BoardColumn> columns = boardColumnRepository.findByBoardIdOrderByPositionAsc(board.getId());
                    columns.stream()
                            .filter(c -> targetColumnTitle.equals(c.getTitle()))
                            .findFirst()
                            .ifPresent(col -> {
                                card.setColumn(col);
                                card.setStage(col.getTitle());
                                int pos = cardRepository.countByColumnId(col.getId()) + 1;
                                card.setPosition(pos);
                                cardRepository.save(card);
                            });
                }
            }
        }
    }

    private String mapStatusToColumnTitle(OrderStatus status) {
        return switch (status) {
            case DRAFT -> "Draft";
            case NEW_ORDER -> "New Order";
            case CLARIFY_WITH_CUSTOMER -> "Clarify with Customer";
            case CONFIRMED -> "Confirmed";
            case DESIGNING -> "Designing";
            case DESIGN_CHECKING -> "Design Checking";
            case NEED_TO_FIX -> "Need to Fix";
            case FIX_CHECKING -> "Fix Checking";
            case FIXING -> "Fixing";
            case CONFIRMING_DESIGN_WITH_CUSTOMER -> "Confirming Design with Customer";
            case DESIGN_APPROVED -> "Design Approved";
            case FULFILLED -> "Fulfilled";
            case TRACK_GENERATED -> "Track Generated";
            case TRACK_ADDED_TO_STORE -> "Track Added to Store";
            default -> null;
        };
    }

    private String normalizeForMatch(String name, String street) {
        if (name == null || street == null) return null;
        String n = name.toLowerCase().trim().replaceAll("\\s+", " ");
        String s = street.toLowerCase().trim().replaceAll("\\s+", " ");
        if (n.isEmpty() || s.isEmpty()) return null;
        return n + "|" + s;
    }
}
