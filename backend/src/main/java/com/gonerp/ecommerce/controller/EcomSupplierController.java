package com.gonerp.ecommerce.controller;

import com.gonerp.common.ApiResponse;
import com.gonerp.ecommerce.dto.EcomSupplierRequest;
import com.gonerp.ecommerce.dto.EcomSupplierResponse;
import com.gonerp.ecommerce.service.EcomSupplierService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ecommerce/suppliers")
@RequiredArgsConstructor
public class EcomSupplierController {

    private final EcomSupplierService supplierService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<EcomSupplierResponse>>> findAll() {
        return ResponseEntity.ok(ApiResponse.ok(supplierService.findAll()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<EcomSupplierResponse>> findById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(supplierService.findById(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<EcomSupplierResponse>> create(@RequestBody EcomSupplierRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Supplier created", supplierService.create(request)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<EcomSupplierResponse>> update(
            @PathVariable Long id, @RequestBody EcomSupplierRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Supplier updated", supplierService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        supplierService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok("Supplier deleted", null));
    }
}
