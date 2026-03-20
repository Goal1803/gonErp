package com.gonerp.finance.controller;

import com.gonerp.common.ApiResponse;
import com.gonerp.finance.dto.FinanceUserRoleRequest;
import com.gonerp.finance.dto.FinanceUserRoleResponse;
import com.gonerp.finance.service.FinanceUserRoleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/finance/config")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
public class FinanceConfigController {

    private final FinanceUserRoleService financeUserRoleService;

    @GetMapping("/roles")
    public ResponseEntity<ApiResponse<List<FinanceUserRoleResponse>>> getAllRoles() {
        return ResponseEntity.ok(ApiResponse.ok(financeUserRoleService.findAllForOrg()));
    }

    @PostMapping("/roles")
    public ResponseEntity<ApiResponse<FinanceUserRoleResponse>> assignRole(
            @Valid @RequestBody FinanceUserRoleRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Role assigned", financeUserRoleService.assign(request)));
    }

    @DeleteMapping("/roles/{userId}")
    public ResponseEntity<ApiResponse<Void>> removeRole(@PathVariable Long userId) {
        financeUserRoleService.remove(userId);
        return ResponseEntity.ok(ApiResponse.ok("Role removed", null));
    }
}
