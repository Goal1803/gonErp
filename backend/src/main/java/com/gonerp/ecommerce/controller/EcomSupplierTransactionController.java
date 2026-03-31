package com.gonerp.ecommerce.controller;

import com.gonerp.common.ApiResponse;
import com.gonerp.ecommerce.dto.EcomSupplierTransactionResponse;
import com.gonerp.ecommerce.service.EcomSupplierTransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ecommerce/suppliers/{supplierId}/transactions")
@RequiredArgsConstructor
public class EcomSupplierTransactionController {

    private final EcomSupplierTransactionService service;

    @GetMapping
    public ResponseEntity<ApiResponse<List<EcomSupplierTransactionResponse>>> findAll(@PathVariable Long supplierId) {
        return ResponseEntity.ok(ApiResponse.ok(service.findAll(supplierId)));
    }

    @PostMapping("/upload")
    public ResponseEntity<ApiResponse<Map<String, Object>>> upload(
            @PathVariable Long supplierId,
            @RequestParam MultipartFile file) {
        return ResponseEntity.ok(ApiResponse.ok("Uploaded", service.uploadTransactions(supplierId, file)));
    }

    @PostMapping("/match")
    public ResponseEntity<ApiResponse<Map<String, Object>>> match(
            @PathVariable Long supplierId,
            @RequestParam(required = false) BigDecimal exchangeRate) {
        return ResponseEntity.ok(ApiResponse.ok("Matched", service.matchToOrders(supplierId, exchangeRate)));
    }

    @PostMapping("/{transactionId}/match-to/{orderId}")
    public ResponseEntity<ApiResponse<Void>> manualMatch(
            @PathVariable Long supplierId,
            @PathVariable Long transactionId,
            @PathVariable Long orderId,
            @RequestParam(required = false) BigDecimal exchangeRate) {
        service.manualMatch(supplierId, transactionId, orderId, exchangeRate);
        return ResponseEntity.ok(ApiResponse.ok("Manually matched", null));
    }
}
