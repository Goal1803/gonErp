package com.gonerp.ecommerce.service;

import com.gonerp.ecommerce.dto.EcomOrderImportResult;
import com.gonerp.ecommerce.dto.EcomOrderImportResult.RowOutcome;
import com.gonerp.ecommerce.model.EcomStore;
import com.gonerp.ecommerce.model.enums.StoreRole;
import com.gonerp.ecommerce.parser.EtsyOrderParser;
import com.gonerp.ecommerce.repository.EcomStoreRepository;
import com.gonerp.ecommerce.service.EcomOrderImportHelper.BatchResult;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class EcomOrderImportService {

    private final EcomStoreRepository ecomStoreRepository;
    private final EcomAccessService ecomAccessService;
    private final EtsyOrderParser etsyOrderParser;
    private final EcomOrderImportHelper importHelper;

    public EcomOrderImportResult importEtsyOrders(Long storeId, MultipartFile ordersFile, MultipartFile itemsFile, Long boardId) {
        ecomAccessService.requireEcommerceAccess();
        ecomAccessService.requireStoreRole(storeId, StoreRole.STORE_ADMIN, StoreRole.SELLER);

        EcomStore store = ecomStoreRepository.findById(storeId)
                .orElseThrow(() -> new EntityNotFoundException("Store not found: " + storeId));

        List<RowOutcome> ignoredRows = new ArrayList<>();
        int totalRowsParsed = 0;

        try {
            // Parse orders CSV — indexed by Order ID
            Map<String, Map<String, String>> ordersMap = new LinkedHashMap<>();
            if (ordersFile != null && !ordersFile.isEmpty()) {
                List<Map<String, String>> orderRows = etsyOrderParser.parseOrders(ordersFile.getInputStream());
                totalRowsParsed += orderRows.size();
                int rowNum = 1;
                for (Map<String, String> row : orderRows) {
                    rowNum++;
                    String orderId = row.get("Order ID");
                    if (orderId == null || orderId.isBlank()) {
                        ignoredRows.add(RowOutcome.builder()
                                .orderId("Row " + rowNum)
                                .reason("Missing Order ID")
                                .detail("Orders CSV row has no Order ID value")
                                .build());
                    } else {
                        ordersMap.put(orderId.trim(), row);
                    }
                }
            }

            // Parse items CSV — grouped by Order ID
            Map<String, List<Map<String, String>>> itemsByOrderId = new LinkedHashMap<>();
            if (itemsFile != null && !itemsFile.isEmpty()) {
                List<Map<String, String>> itemRows = etsyOrderParser.parseOrderItems(itemsFile.getInputStream());
                totalRowsParsed += itemRows.size();
                int rowNum = 1;
                for (Map<String, String> row : itemRows) {
                    rowNum++;
                    String orderId = row.get("Order ID");
                    if (orderId == null || orderId.isBlank()) {
                        String itemName = row.getOrDefault("Item Name", "");
                        String txnId = row.getOrDefault("Transaction ID", "");
                        ignoredRows.add(RowOutcome.builder()
                                .orderId("Items Row " + rowNum)
                                .reason("Missing Order ID")
                                .detail("Item: " + importHelper.truncate(itemName, 60) + (txnId.isBlank() ? "" : " (Txn: " + txnId + ")"))
                                .build());
                    } else {
                        itemsByOrderId.computeIfAbsent(orderId.trim(), k -> new ArrayList<>()).add(row);
                    }
                }
            }

            // Determine which Order IDs to process
            Set<String> allOrderIds = new LinkedHashSet<>();
            allOrderIds.addAll(ordersMap.keySet());
            allOrderIds.addAll(itemsByOrderId.keySet());

            // Process all orders in a single transaction with flush/clear per order
            BatchResult batch = importHelper.processAllOrders(store, allOrderIds, ordersMap, itemsByOrderId, boardId);

            return EcomOrderImportResult.builder()
                    .ordersCreated(batch.created().size())
                    .ordersUpdated(batch.updated().size())
                    .orderItemsImported(batch.itemsImported())
                    .skipped(batch.skippedRows().size())
                    .totalRowsParsed(totalRowsParsed)
                    .created(batch.created())
                    .updated(batch.updated())
                    .skippedRows(batch.skippedRows())
                    .errors(batch.errors())
                    .ignoredRows(ignoredRows)
                    .build();

        } catch (Exception e) {
            log.error("Failed to parse CSV files for store {}", storeId, e);
            throw new RuntimeException("Failed to parse CSV files: " + e.getMessage(), e);
        }
    }
}
