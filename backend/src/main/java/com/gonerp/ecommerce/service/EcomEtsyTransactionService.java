package com.gonerp.ecommerce.service;

import com.gonerp.ecommerce.model.EcomEtsyTransaction;
import com.gonerp.ecommerce.model.EcomOrder;
import com.gonerp.ecommerce.model.EcomStore;
import com.gonerp.ecommerce.model.enums.StoreRole;
import com.gonerp.ecommerce.repository.EcomEtsyTransactionRepository;
import com.gonerp.ecommerce.repository.EcomOrderRepository;
import com.gonerp.ecommerce.repository.EcomStoreRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class EcomEtsyTransactionService {

    private final EcomEtsyTransactionRepository txnRepository;
    private final EcomOrderRepository orderRepository;
    private final EcomStoreRepository storeRepository;
    private final EcomAccessService accessService;

    private static final Pattern ORDER_ID_PATTERN = Pattern.compile("Order #(\\d+)");
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("MMMM d, yyyy", Locale.ENGLISH);

    /**
     * Upload and parse Etsy statement CSV. Deduplicates on re-upload.
     */
    public Map<String, Object> uploadStatement(Long storeId, MultipartFile file) {
        accessService.requireEcommerceAccess();
        accessService.requireStoreRole(storeId, StoreRole.STORE_ADMIN, StoreRole.SELLER);

        EcomStore store = storeRepository.findById(storeId)
                .orElseThrow(() -> new EntityNotFoundException("Store not found: " + storeId));

        int inserted = 0;
        int skipped = 0;
        int errors = 0;

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), "UTF-8"))) {

            List<Map<String, String>> rows = parseCsv(reader);

            for (Map<String, String> row : rows) {
                try {
                    String type = clean(row.get("Type"));
                    String title = clean(row.get("Title"));
                    String info = clean(row.get("Info"));
                    String currency = clean(row.get("Currency"));
                    BigDecimal amount = parseEuroAmount(row.get("Amount"));
                    BigDecimal feesAndTaxes = parseEuroAmount(row.get("Fees & Taxes"));
                    BigDecimal net = parseEuroAmount(row.get("Net"));
                    LocalDate txnDate = parseDate(clean(row.get("Date")));

                    if (type == null || type.isBlank()) continue;

                    // Dedup check
                    if (txnRepository.existsByCompositeKey(
                            storeId, txnDate, type, truncate(title, 1000), truncate(info, 1000), amount, feesAndTaxes, currency)) {
                        skipped++;
                        continue;
                    }

                    // Extract order ID from Info or Title
                    String orderIdRef = extractOrderId(info);
                    if (orderIdRef == null) orderIdRef = extractOrderId(title);

                    EcomEtsyTransaction txn = EcomEtsyTransaction.builder()
                            .store(store)
                            .txnDate(txnDate)
                            .type(type)
                            .title(truncate(title, 1000))
                            .info(truncate(info, 1000))
                            .currency(currency)
                            .amount(amount)
                            .feesAndTaxes(feesAndTaxes)
                            .net(net)
                            .taxDetails(truncate(clean(row.get("Tax Details")), 500))
                            .status(clean(row.get("Status")))
                            .availabilityDate(truncate(clean(row.get("Availability Date")), 200))
                            .orderIdRef(orderIdRef)
                            .build();
                    txnRepository.save(txn);
                    inserted++;
                } catch (Exception e) {
                    log.warn("Failed to parse statement row: {}", e.getMessage());
                    errors++;
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse statement CSV: " + e.getMessage(), e);
        }

        return Map.of("inserted", inserted, "skipped", skipped, "errors", errors);
    }

    /**
     * Auto-match transaction fees to orders. For each order with unmatched transactions,
     * sum Fee+Tax rows and calculate earning after platform fee.
     */
    public Map<String, Object> matchFeesToOrders(Long storeId) {
        accessService.requireEcommerceAccess();

        List<String> unmatchedOrderIds = txnRepository.findUnmatchedOrderIds(storeId);
        int matched = 0;
        int skippedNoOrder = 0;
        int skippedAlreadyHasFee = 0;

        for (String orderIdRef : unmatchedOrderIds) {
            Optional<EcomOrder> orderOpt = orderRepository.findByPlatformOrderIdAndStoreId(orderIdRef, storeId);
            if (orderOpt.isEmpty()) {
                skippedNoOrder++;
                continue;
            }

            EcomOrder order = orderOpt.get();
            if (order.getPlatformFee() != null) {
                // Already has fee — mark transactions as matched but don't recalculate
                List<EcomEtsyTransaction> txns = txnRepository.findByStoreIdAndOrderIdRef(storeId, orderIdRef);
                txns.forEach(t -> t.setMatched(true));
                txnRepository.saveAll(txns);
                skippedAlreadyHasFee++;
                continue;
            }

            List<EcomEtsyTransaction> txns = txnRepository.findByStoreIdAndOrderIdRef(storeId, orderIdRef);

            BigDecimal totalFees = BigDecimal.ZERO;
            BigDecimal saleAmount = BigDecimal.ZERO;
            BigDecimal refundAmount = BigDecimal.ZERO;

            for (EcomEtsyTransaction txn : txns) {
                String type = txn.getType();
                if ("Fee".equals(type) || "Tax".equals(type)) {
                    if (txn.getFeesAndTaxes() != null) {
                        totalFees = totalFees.add(txn.getFeesAndTaxes());
                    }
                } else if ("Sale".equals(type)) {
                    if (txn.getAmount() != null) {
                        saleAmount = saleAmount.add(txn.getAmount());
                    }
                } else if ("Refund".equals(type)) {
                    if (txn.getAmount() != null) {
                        refundAmount = refundAmount.add(txn.getAmount());
                    }
                    if (txn.getFeesAndTaxes() != null) {
                        totalFees = totalFees.add(txn.getFeesAndTaxes());
                    }
                } else if ("Buyer Fee".equals(type)) {
                    if (txn.getFeesAndTaxes() != null) {
                        totalFees = totalFees.add(txn.getFeesAndTaxes());
                    }
                }
            }

            // platformFee is the absolute value of total fees (stored as positive)
            order.setPlatformFee(totalFees.abs());
            // earning = sale + refund + fees (fees/refund are negative)
            BigDecimal earning = saleAmount.add(refundAmount).add(totalFees);
            order.setEarningAfterPlatformFee(earning);

            // Store refund amount (as positive value) and auto-set refunded flag
            if (refundAmount.compareTo(BigDecimal.ZERO) < 0) {
                order.setRefundAmount(refundAmount.abs());
                order.setRefunded(true);
            }

            // Recalculate gross profit
            BigDecimal profit = earning;
            if (order.getFulfillmentCost() != null) profit = profit.subtract(order.getFulfillmentCost());
            if (order.getOtherCost() != null) profit = profit.subtract(order.getOtherCost());
            order.setGrossProfit(profit);

            orderRepository.save(order);

            // Mark all transactions as matched
            txns.forEach(t -> t.setMatched(true));
            txnRepository.saveAll(txns);
            matched++;
        }

        return Map.of("matched", matched, "skippedNoOrder", skippedNoOrder,
                "skippedAlreadyHasFee", skippedAlreadyHasFee, "total", unmatchedOrderIds.size());
    }

    /**
     * Get all transactions for a store, with optional filters.
     */
    public List<com.gonerp.ecommerce.dto.EcomEtsyTransactionResponse> findAll(Long storeId) {
        accessService.requireEcommerceAccess();
        return txnRepository.findByStoreIdOrderByTxnDateDesc(storeId).stream()
                .map(com.gonerp.ecommerce.dto.EcomEtsyTransactionResponse::from)
                .toList();
    }

    // ===== Parsing helpers =====

    private List<Map<String, String>> parseCsv(BufferedReader reader) throws IOException {
        List<Map<String, String>> rows = new ArrayList<>();
        String headerLine = reader.readLine();
        if (headerLine == null) return rows;

        // Remove BOM
        headerLine = headerLine.replace("\uFEFF", "").replace("\ufeff", "");
        String[] headers = parseCsvLine(headerLine);

        String line;
        StringBuilder multiLine = new StringBuilder();
        while ((line = reader.readLine()) != null) {
            multiLine.append(line);
            // Check if we have balanced quotes
            if (isBalancedQuotes(multiLine.toString())) {
                String[] values = parseCsvLine(multiLine.toString());
                Map<String, String> row = new LinkedHashMap<>();
                for (int i = 0; i < headers.length && i < values.length; i++) {
                    row.put(headers[i].trim(), values[i]);
                }
                rows.add(row);
                multiLine = new StringBuilder();
            } else {
                multiLine.append("\n");
            }
        }
        return rows;
    }

    private String[] parseCsvLine(String line) {
        List<String> values = new ArrayList<>();
        boolean inQuotes = false;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    sb.append('"');
                    i++;
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (c == ',' && !inQuotes) {
                values.add(sb.toString().trim());
                sb = new StringBuilder();
            } else {
                sb.append(c);
            }
        }
        values.add(sb.toString().trim());
        return values.toArray(new String[0]);
    }

    private boolean isBalancedQuotes(String s) {
        long count = s.chars().filter(c -> c == '"').count();
        return count % 2 == 0;
    }

    private BigDecimal parseEuroAmount(String value) {
        if (value == null) return null;
        String cleaned = value.trim();
        if (cleaned.equals("--") || cleaned.isEmpty()) return null;
        // Remove currency symbols (€, $) and any unicode chars
        cleaned = cleaned.replaceAll("[^\\d.,-]", "");
        if (cleaned.isEmpty()) return null;
        // Handle European format
        if (cleaned.contains(",") && cleaned.contains(".") && cleaned.indexOf(',') < cleaned.lastIndexOf('.')) {
            cleaned = cleaned.replace(",", "");
        } else if (cleaned.contains(",") && !cleaned.contains(".")) {
            cleaned = cleaned.replace(",", ".");
        }
        try {
            return new BigDecimal(cleaned);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.isBlank()) return null;
        try {
            return LocalDate.parse(dateStr.trim(), DATE_FORMAT);
        } catch (Exception e) {
            return null;
        }
    }

    private String extractOrderId(String text) {
        if (text == null) return null;
        Matcher m = ORDER_ID_PATTERN.matcher(text);
        return m.find() ? m.group(1) : null;
    }

    private String clean(String s) {
        if (s == null) return null;
        return s.trim().isEmpty() ? null : s.trim();
    }

    private String truncate(String s, int max) {
        if (s == null) return null;
        return s.length() <= max ? s : s.substring(0, max);
    }
}
