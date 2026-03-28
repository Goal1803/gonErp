package com.gonerp.ecommerce.controller;

import com.gonerp.common.ApiResponse;
import com.gonerp.ecommerce.dto.EcomOrderRequest;
import com.gonerp.ecommerce.dto.EcomOrderResponse;
import com.gonerp.ecommerce.service.EcomOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/ecommerce/orders")
@RequiredArgsConstructor
public class EcomOrderController {

    private final EcomOrderService ecomOrderService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<EcomOrderResponse>>> findAll(
            @RequestParam(required = false) Long storeId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        return ResponseEntity.ok(ApiResponse.ok(ecomOrderService.findAll(storeId, status, startDate, endDate)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<EcomOrderResponse>> findById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(ecomOrderService.findById(id)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<EcomOrderResponse>> update(
            @PathVariable Long id, @RequestBody EcomOrderRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Order updated", ecomOrderService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        ecomOrderService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok("Order deleted", null));
    }
}
