package com.gonerp.finance.controller;

import com.gonerp.common.ApiResponse;
import com.gonerp.finance.dto.FinanceAccountRequest;
import com.gonerp.finance.dto.FinanceAccountResponse;
import com.gonerp.finance.service.FinanceAccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/finance/accounts")
@RequiredArgsConstructor
public class FinanceAccountController {

    private final FinanceAccountService financeAccountService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<FinanceAccountResponse>>> findAll() {
        return ResponseEntity.ok(ApiResponse.ok(financeAccountService.findAll()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<FinanceAccountResponse>> findById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(financeAccountService.findById(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<FinanceAccountResponse>> create(
            @Valid @RequestBody FinanceAccountRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Account created", financeAccountService.create(request)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<FinanceAccountResponse>> update(
            @PathVariable Long id, @Valid @RequestBody FinanceAccountRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Account updated", financeAccountService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        financeAccountService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok("Account deleted", null));
    }
}
