package com.gonerp.ecommerce.controller;

import com.gonerp.common.ApiResponse;
import com.gonerp.ecommerce.dto.EcomOrderImportResult;
import com.gonerp.ecommerce.service.EcomOrderImportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/ecommerce/import")
@RequiredArgsConstructor
public class EcomOrderImportController {

    private final EcomOrderImportService ecomOrderImportService;

    @PostMapping("/etsy/{storeId}")
    public ResponseEntity<ApiResponse<EcomOrderImportResult>> importEtsy(
            @PathVariable Long storeId,
            @RequestParam(required = false) MultipartFile ordersFile,
            @RequestParam(required = false) MultipartFile itemsFile,
            @RequestParam(required = false) Long boardId) {
        if ((ordersFile == null || ordersFile.isEmpty()) && (itemsFile == null || itemsFile.isEmpty())) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("At least one CSV file (orders or items) is required"));
        }
        if (boardId != null) {
            if (ordersFile == null || ordersFile.isEmpty() || itemsFile == null || itemsFile.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Both orders and items CSV files are required when syncing to a board"));
            }
        }
        EcomOrderImportResult result = ecomOrderImportService.importEtsyOrders(storeId, ordersFile, itemsFile, boardId);
        return ResponseEntity.ok(ApiResponse.ok("Import completed", result));
    }
}
