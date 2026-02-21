package com.gonerp.organization.controller;

import com.gonerp.common.ApiResponse;
import com.gonerp.organization.dto.OrganizationRequest;
import com.gonerp.organization.dto.OrganizationResponse;
import com.gonerp.organization.service.OrganizationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/organizations")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class OrganizationController {

    private final OrganizationService organizationService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<OrganizationResponse>>> findAll() {
        return ResponseEntity.ok(ApiResponse.ok(organizationService.findAll()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<OrganizationResponse>> findById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(organizationService.findById(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<OrganizationResponse>> create(
            @Valid @RequestBody OrganizationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Organization created", organizationService.create(request)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<OrganizationResponse>> update(
            @PathVariable Long id, @Valid @RequestBody OrganizationRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Organization updated", organizationService.update(id, request)));
    }

    @PatchMapping("/{id}/toggle-active")
    public ResponseEntity<ApiResponse<OrganizationResponse>> toggleActive(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok("Organization toggled", organizationService.toggleActive(id)));
    }
}
