package com.gonerp.ecommerce.controller;

import com.gonerp.common.ApiResponse;
import com.gonerp.ecommerce.dto.EcomEtsyTransactionResponse;
import com.gonerp.ecommerce.service.EcomEtsyTransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ecommerce/stores/{storeId}/transactions")
@RequiredArgsConstructor
public class EcomEtsyTransactionController {

    private final EcomEtsyTransactionService transactionService;

    @PostMapping("/upload")
    public ResponseEntity<ApiResponse<Map<String, Object>>> upload(
            @PathVariable Long storeId,
            @RequestParam MultipartFile file) {
        return ResponseEntity.ok(ApiResponse.ok("Statement uploaded", transactionService.uploadStatement(storeId, file)));
    }

    @PostMapping("/match-fees")
    public ResponseEntity<ApiResponse<Map<String, Object>>> matchFees(@PathVariable Long storeId) {
        return ResponseEntity.ok(ApiResponse.ok("Fees matched", transactionService.matchFeesToOrders(storeId)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<EcomEtsyTransactionResponse>>> findAll(@PathVariable Long storeId) {
        return ResponseEntity.ok(ApiResponse.ok(transactionService.findAll(storeId)));
    }
}
