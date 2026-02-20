package com.gonerp.taskmanager.controller;

import com.gonerp.common.ApiResponse;
import com.gonerp.taskmanager.dto.LookupRequest;
import com.gonerp.taskmanager.dto.LookupResponse;
import com.gonerp.taskmanager.service.LookupService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
public class LookupController {

    private final LookupService lookupService;

    // --- Product Types ---

    @GetMapping("/product-types")
    public ResponseEntity<ApiResponse<List<LookupResponse>>> getAllProductTypes() {
        return ResponseEntity.ok(ApiResponse.ok(lookupService.findAllProductTypes()));
    }

    @PostMapping("/product-types")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<LookupResponse>> createProductType(@Valid @RequestBody LookupRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Product type created", lookupService.createProductType(request)));
    }

    @PutMapping("/product-types/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<LookupResponse>> updateProductType(
            @PathVariable Long id, @Valid @RequestBody LookupRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Product type updated", lookupService.updateProductType(id, request)));
    }

    @DeleteMapping("/product-types/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteProductType(@PathVariable Long id) {
        lookupService.deleteProductType(id);
        return ResponseEntity.ok(ApiResponse.ok("Product type deleted", null));
    }

    // --- Niches ---

    @GetMapping("/niches")
    public ResponseEntity<ApiResponse<List<LookupResponse>>> getAllNiches() {
        return ResponseEntity.ok(ApiResponse.ok(lookupService.findAllNiches()));
    }

    @PostMapping("/niches")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<LookupResponse>> createNiche(@Valid @RequestBody LookupRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Niche created", lookupService.createNiche(request)));
    }

    @PutMapping("/niches/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<LookupResponse>> updateNiche(
            @PathVariable Long id, @Valid @RequestBody LookupRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Niche updated", lookupService.updateNiche(id, request)));
    }

    @DeleteMapping("/niches/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteNiche(@PathVariable Long id) {
        lookupService.deleteNiche(id);
        return ResponseEntity.ok(ApiResponse.ok("Niche deleted", null));
    }

    // --- Occasions ---

    @GetMapping("/occasions")
    public ResponseEntity<ApiResponse<List<LookupResponse>>> getAllOccasions() {
        return ResponseEntity.ok(ApiResponse.ok(lookupService.findAllOccasions()));
    }

    @PostMapping("/occasions")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<LookupResponse>> createOccasion(@Valid @RequestBody LookupRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Occasion created", lookupService.createOccasion(request)));
    }

    @PutMapping("/occasions/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<LookupResponse>> updateOccasion(
            @PathVariable Long id, @Valid @RequestBody LookupRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Occasion updated", lookupService.updateOccasion(id, request)));
    }

    @DeleteMapping("/occasions/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteOccasion(@PathVariable Long id) {
        lookupService.deleteOccasion(id);
        return ResponseEntity.ok(ApiResponse.ok("Occasion deleted", null));
    }
}
