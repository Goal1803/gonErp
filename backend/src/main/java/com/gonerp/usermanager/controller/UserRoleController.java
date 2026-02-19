package com.gonerp.usermanager.controller;

import com.gonerp.common.ApiResponse;
import com.gonerp.usermanager.dto.UserRoleRequest;
import com.gonerp.usermanager.dto.UserRoleResponse;
import com.gonerp.usermanager.service.UserRoleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/user-roles")
@RequiredArgsConstructor
public class UserRoleController {

    private final UserRoleService userRoleService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<UserRoleResponse>>> findAll() {
        return ResponseEntity.ok(ApiResponse.ok(userRoleService.findAll()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserRoleResponse>> findById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(userRoleService.findById(id)));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserRoleResponse>> create(@Valid @RequestBody UserRoleRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("UserRole created successfully", userRoleService.create(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserRoleResponse>> update(@PathVariable Long id,
                                                                @Valid @RequestBody UserRoleRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("UserRole updated successfully", userRoleService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        userRoleService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok("UserRole deleted successfully", null));
    }
}
