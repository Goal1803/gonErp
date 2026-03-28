package com.gonerp.ecommerce.controller;

import com.gonerp.common.ApiResponse;
import com.gonerp.ecommerce.dto.EcomStoreRequest;
import com.gonerp.ecommerce.dto.EcomStoreResponse;
import com.gonerp.ecommerce.service.EcomStoreService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ecommerce/stores")
@RequiredArgsConstructor
public class EcomStoreController {

    private final EcomStoreService ecomStoreService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<EcomStoreResponse>>> findAll() {
        return ResponseEntity.ok(ApiResponse.ok(ecomStoreService.findAll()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<EcomStoreResponse>> findById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(ecomStoreService.findById(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<EcomStoreResponse>> create(
            @Valid @RequestBody EcomStoreRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Store created", ecomStoreService.create(request)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<EcomStoreResponse>> update(
            @PathVariable Long id, @Valid @RequestBody EcomStoreRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Store updated", ecomStoreService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        ecomStoreService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok("Store deleted", null));
    }
}
