package com.gonerp.ecommerce.service;

import com.gonerp.ecommerce.dto.EcomOrderImportResult.RowOutcome;
import com.gonerp.ecommerce.model.EcomOrder;
import com.gonerp.ecommerce.model.EcomOrderItem;
import com.gonerp.ecommerce.model.EcomStore;
import com.gonerp.ecommerce.model.enums.OrderStatus;
import com.gonerp.ecommerce.model.enums.SalesChannel;
import com.gonerp.ecommerce.repository.EcomOrderRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class EcomOrderImportHelper {

    private final EcomOrderRepository ecomOrderRepository;
    private final EntityManager entityManager;

    /**
     * Process all orders in a single transaction, flushing after each order
     * to isolate errors without exhausting the connection pool.
     */
    @Transactional
    public BatchResult processAllOrders(EcomStore store,
                                         Set<String> allOrderIds,
                                         Map<String, Map<String, String>> ordersMap,
                                         Map<String, List<Map<String, String>>> itemsByOrderId) {
        List<RowOutcome> created = new ArrayList<>();
        List<RowOutcome> updated = new ArrayList<>();
        List<RowOutcome> skippedRows = new ArrayList<>();
        List<RowOutcome> errors = new ArrayList<>();
        int itemsImported = 0;

        for (String orderId : allOrderIds) {
            try {
                Map<String, String> orderRow = ordersMap.get(orderId);
                List<Map<String, String>> itemRows = itemsByOrderId.getOrDefault(orderId, Collections.emptyList());

                Optional<EcomOrder> existingOpt = ecomOrderRepository.findByPlatformOrderIdAndStoreId(orderId, store.getId());

                if (existingOpt.isPresent()) {
                    EcomOrder existing = existingOpt.get();
                    boolean mergedFinancials = false;
                    boolean mergedItems = false;
                    List<String> updateDetails = new ArrayList<>();
                    int newItems = 0;

                    if (orderRow != null && existing.getOrderTotal() == null) {
                        mergeOrderFinancials(existing, orderRow);
                        mergedFinancials = true;
                        updateDetails.add("merged financials");
                    }

                    if (!itemRows.isEmpty() && (existing.getItems() == null || existing.getItems().isEmpty() || hasOnlyPlaceholderItems(existing))) {
                        // Remove placeholder items before adding real ones
                        if (hasOnlyPlaceholderItems(existing)) {
                            existing.getItems().clear();
                        }
                        for (Map<String, String> itemRow : itemRows) {
                            EcomOrderItem item = buildOrderItem(existing, itemRow);
                            existing.getItems().add(item);
                            newItems++;
                        }
                        mergedItems = true;
                        updateDetails.add("added " + newItems + " items");
                    }

                    if (mergedFinancials || mergedItems) {
                        ecomOrderRepository.save(existing);
                        entityManager.flush();
                        itemsImported += newItems;
                        updated.add(RowOutcome.builder()
                                .orderId(orderId)
                                .reason("Updated existing order")
                                .detail(String.join(", ", updateDetails))
                                .build());
                    } else {
                        List<String> skipReasons = new ArrayList<>();
                        if (orderRow != null && existing.getOrderTotal() != null)
                            skipReasons.add("financials already present");
                        if (!itemRows.isEmpty() && existing.getItems() != null && !existing.getItems().isEmpty() && !hasOnlyPlaceholderItems(existing))
                            skipReasons.add("items already present (" + existing.getItems().size() + ")");
                        if (orderRow == null && itemRows.isEmpty())
                            skipReasons.add("no new data");
                        skippedRows.add(RowOutcome.builder()
                                .orderId(orderId)
                                .reason("Already exists — no changes")
                                .detail(skipReasons.isEmpty() ? "Duplicate" : String.join("; ", skipReasons))
                                .build());
                    }
                } else {
                    EcomOrder order = buildOrder(store, orderId, orderRow, itemRows);
                    ecomOrderRepository.save(order);
                    entityManager.flush();
                    int itemCount = order.getItems().size();
                    itemsImported += itemCount;

                    String detail = orderRow != null ? "with financials" : "from items only";
                    if (itemCount > 0) detail += ", " + itemCount + " items";
                    created.add(RowOutcome.builder()
                            .orderId(orderId)
                            .reason("New order created")
                            .detail(detail)
                            .build());
                }

                // Detach saved entities to keep persistence context clean
                entityManager.clear();

            } catch (Exception e) {
                log.warn("Failed to import order {}: {}", orderId, e.getMessage());
                // Clear the corrupted session so next order starts fresh
                entityManager.clear();
                errors.add(RowOutcome.builder()
                        .orderId(orderId)
                        .reason("Error processing order")
                        .detail(e.getMessage())
                        .build());
            }
        }

        return new BatchResult(created, updated, skippedRows, errors, itemsImported);
    }

    // ===================== builders =====================

    EcomOrder buildOrder(EcomStore store, String orderId,
                         Map<String, String> orderRow, List<Map<String, String>> itemRows) {
        EcomOrder.EcomOrderBuilder builder = EcomOrder.builder()
                .organization(store.getOrganization())
                .store(store)
                .salesChannel(SalesChannel.ETSY)
                .platformOrderId(orderId)
                .status(OrderStatus.NEW_ORDER);

        if (orderRow != null) {
            builder.customerName(getField(orderRow, "Full Name"));
            builder.buyerUserId(getField(orderRow, "Buyer User ID"));
            builder.shipStreet1(getField(orderRow, "Street 1"));
            builder.shipStreet2(getField(orderRow, "Street 2"));
            builder.shipCity(getField(orderRow, "Ship City"));
            builder.shipState(getField(orderRow, "Ship State"));
            builder.shipZipcode(getField(orderRow, "Ship Zipcode"));
            builder.shipCountry(getField(orderRow, "Ship Country"));
            builder.currency(getFieldOrDefault(orderRow, "Currency", store.getCurrency()));
            builder.orderValue(parseAmount(getField(orderRow, "Order Value")));
            builder.shippingPrice(parseAmount(getField(orderRow, "Shipping")));
            builder.tax(parseAmount(getField(orderRow, "Sales Tax")));
            builder.discount(parseAmount(getField(orderRow, "Discount Amount")));
            builder.processingFees(parseAmount(getField(orderRow, "Card Processing Fees")));
            builder.orderTotal(parseAmount(getField(orderRow, "Order Total")));
            builder.orderNet(parseAmount(getField(orderRow, "Order Net")));
            builder.numberOfItems(parseIntSafe(getField(orderRow, "Number of Items"), 1));
            builder.sku(getField(orderRow, "SKU"));

            String saleDate = getField(orderRow, "Sale Date");
            if (saleDate != null) builder.orderDate(parseEtsyDate(saleDate));

            String shippedDate = getField(orderRow, "Date Shipped");
            if (shippedDate != null) builder.shippedDate(parseEtsyDate(shippedDate));

            builder.rawData(new LinkedHashMap<>(orderRow));

        } else if (!itemRows.isEmpty()) {
            Map<String, String> firstItem = itemRows.get(0);
            builder.customerName(getField(firstItem, "Ship Name"));
            builder.shipStreet1(getField(firstItem, "Ship Address1"));
            builder.shipStreet2(getField(firstItem, "Ship Address2"));
            builder.shipCity(getField(firstItem, "Ship City"));
            builder.shipState(getField(firstItem, "Ship State"));
            builder.shipZipcode(getField(firstItem, "Ship Zipcode"));
            builder.shipCountry(getField(firstItem, "Ship Country"));
            builder.currency(getFieldOrDefault(firstItem, "Currency", store.getCurrency()));
            builder.numberOfItems(itemRows.size());

            String saleDate = getField(firstItem, "Sale Date");
            if (saleDate != null) builder.orderDate(parseEtsyDate(saleDate));
        }

        Map<String, Object> platformData = new LinkedHashMap<>();
        platformData.put("etsyOrderId", orderId);
        if (orderRow != null) {
            platformData.put("buyerUserId", getField(orderRow, "Buyer User ID"));
            platformData.put("paymentMethod", getField(orderRow, "Payment Method"));
            platformData.put("couponCode", getField(orderRow, "Coupon Code"));
            platformData.put("status", getField(orderRow, "Status"));
        }
        builder.platformData(platformData);

        EcomOrder order = builder.build();

        for (Map<String, String> itemRow : itemRows) {
            EcomOrderItem item = EcomOrderItem.builder()
                    .order(order)
                    .platformItemId(getField(itemRow, "Transaction ID"))
                    .productName(getField(itemRow, "Item Name"))
                    .sku(getField(itemRow, "SKU"))
                    .listingId(getField(itemRow, "Listing ID"))
                    .variations(getField(itemRow, "Variations"))
                    .quantity(parseIntSafe(getField(itemRow, "Quantity"), 1))
                    .itemPrice(parseAmount(getField(itemRow, "Price")))
                    .itemTotal(parseAmount(getField(itemRow, "Item Total")))
                    .rawData(new LinkedHashMap<>(itemRow))
                    .build();
            order.getItems().add(item);
        }

        if (order.getItems().isEmpty() && orderRow != null) {
            String sku = getField(orderRow, "SKU");
            if (sku != null) {
                EcomOrderItem item = EcomOrderItem.builder()
                        .order(order)
                        .sku(sku)
                        .quantity(order.getNumberOfItems())
                        .build();
                order.getItems().add(item);
            }
        }

        return order;
    }

    void mergeOrderFinancials(EcomOrder existing, Map<String, String> orderRow) {
        if (existing.getCustomerName() == null) existing.setCustomerName(getField(orderRow, "Full Name"));
        if (existing.getBuyerUserId() == null) existing.setBuyerUserId(getField(orderRow, "Buyer User ID"));
        if (existing.getShipStreet1() == null) existing.setShipStreet1(getField(orderRow, "Street 1"));
        if (existing.getShipStreet2() == null) existing.setShipStreet2(getField(orderRow, "Street 2"));
        if (existing.getShipCity() == null) existing.setShipCity(getField(orderRow, "Ship City"));
        if (existing.getShipState() == null) existing.setShipState(getField(orderRow, "Ship State"));
        if (existing.getShipZipcode() == null) existing.setShipZipcode(getField(orderRow, "Ship Zipcode"));
        if (existing.getShipCountry() == null) existing.setShipCountry(getField(orderRow, "Ship Country"));
        existing.setOrderValue(parseAmount(getField(orderRow, "Order Value")));
        existing.setShippingPrice(parseAmount(getField(orderRow, "Shipping")));
        existing.setTax(parseAmount(getField(orderRow, "Sales Tax")));
        existing.setDiscount(parseAmount(getField(orderRow, "Discount Amount")));
        existing.setProcessingFees(parseAmount(getField(orderRow, "Card Processing Fees")));
        existing.setOrderTotal(parseAmount(getField(orderRow, "Order Total")));
        existing.setOrderNet(parseAmount(getField(orderRow, "Order Net")));
        existing.setNumberOfItems(parseIntSafe(getField(orderRow, "Number of Items"), existing.getNumberOfItems()));
        if (existing.getSku() == null) existing.setSku(getField(orderRow, "SKU"));
        if (existing.getRawData() == null) existing.setRawData(new LinkedHashMap<>(orderRow));

        String shippedDate = getField(orderRow, "Date Shipped");
        if (shippedDate != null && existing.getShippedDate() == null) {
            existing.setShippedDate(parseEtsyDate(shippedDate));
        }
    }

    EcomOrderItem buildOrderItem(EcomOrder order, Map<String, String> itemRow) {
        return EcomOrderItem.builder()
                .order(order)
                .platformItemId(getField(itemRow, "Transaction ID"))
                .productName(getField(itemRow, "Item Name"))
                .sku(getField(itemRow, "SKU"))
                .listingId(getField(itemRow, "Listing ID"))
                .variations(getField(itemRow, "Variations"))
                .quantity(parseIntSafe(getField(itemRow, "Quantity"), 1))
                .itemPrice(parseAmount(getField(itemRow, "Price")))
                .itemTotal(parseAmount(getField(itemRow, "Item Total")))
                .rawData(new LinkedHashMap<>(itemRow))
                .build();
    }

    /**
     * A placeholder item is one created from the Orders CSV only — has SKU but no
     * platformItemId and no productName. These should be replaced when real items arrive.
     */
    private boolean hasOnlyPlaceholderItems(EcomOrder order) {
        if (order.getItems() == null || order.getItems().isEmpty()) return false;
        return order.getItems().stream().allMatch(item ->
                item.getPlatformItemId() == null && item.getProductName() == null);
    }

    // ===================== utilities =====================

    String getField(Map<String, String> row, String key) {
        if (row == null) return null;
        String value = row.get(key);
        return (value != null && !value.isBlank()) ? value.trim() : null;
    }

    String getFieldOrDefault(Map<String, String> row, String key, String defaultValue) {
        String value = getField(row, key);
        return value != null ? value : defaultValue;
    }

    LocalDateTime parseEtsyDate(String dateStr) {
        if (dateStr == null || dateStr.isBlank()) return null;
        String trimmed = dateStr.trim();
        String[] patterns = {"M/d/yy", "MM/dd/yy", "M/d/yyyy", "MM/dd/yyyy"};
        for (String pattern : patterns) {
            try {
                var date = java.time.LocalDate.parse(trimmed, DateTimeFormatter.ofPattern(pattern));
                return date.atStartOfDay();
            } catch (Exception ignored) {}
        }
        return null;
    }

    BigDecimal parseAmount(String amtStr) {
        if (amtStr == null || amtStr.isBlank()) return null;
        String cleaned = amtStr.trim().replaceAll("[^\\d.,-]", "");
        if (cleaned.isEmpty()) return null;
        if (cleaned.contains(",") && cleaned.contains(".") && cleaned.indexOf(',') < cleaned.lastIndexOf('.')) {
            cleaned = cleaned.replace(",", "");
        } else if (cleaned.contains(",") && !cleaned.contains(".")) {
            cleaned = cleaned.replace(",", ".");
        }
        try { return new BigDecimal(cleaned); }
        catch (NumberFormatException e) { return null; }
    }

    int parseIntSafe(String value, int defaultValue) {
        if (value == null || value.isBlank()) return defaultValue;
        try { return Integer.parseInt(value.trim()); }
        catch (NumberFormatException e) { return defaultValue; }
    }

    String truncate(String s, int maxLen) {
        if (s == null) return "";
        return s.length() <= maxLen ? s : s.substring(0, maxLen) + "...";
    }

    // ===================== result holder =====================

    public record BatchResult(
            List<RowOutcome> created,
            List<RowOutcome> updated,
            List<RowOutcome> skippedRows,
            List<RowOutcome> errors,
            int itemsImported
    ) {}
}
