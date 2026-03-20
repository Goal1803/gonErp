package com.gonerp.finance.controller;

import com.gonerp.common.ApiResponse;
import com.gonerp.finance.dto.*;
import com.gonerp.finance.service.CurrencyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/finance/currencies")
@RequiredArgsConstructor
public class CurrencyController {

    private final CurrencyService currencyService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<CurrencyResponse>>> findAll() {
        return ResponseEntity.ok(ApiResponse.ok(currencyService.findAllCurrencies()));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<CurrencyResponse>> create(@Valid @RequestBody CurrencyRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Currency created", currencyService.createCurrency(request)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<CurrencyResponse>> update(@PathVariable Long id, @Valid @RequestBody CurrencyRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Currency updated", currencyService.updateCurrency(id, request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        currencyService.deleteCurrency(id);
        return ResponseEntity.ok(ApiResponse.ok("Currency deleted", null));
    }

    // --- Rates ---

    @GetMapping("/rates")
    public ResponseEntity<ApiResponse<List<CurrencyRateResponse>>> findAllRates() {
        return ResponseEntity.ok(ApiResponse.ok(currencyService.findAllRates()));
    }

    @PostMapping("/rates")
    public ResponseEntity<ApiResponse<CurrencyRateResponse>> createRate(@Valid @RequestBody CurrencyRateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Rate created", currencyService.createRate(request)));
    }

    @DeleteMapping("/rates/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteRate(@PathVariable Long id) {
        currencyService.deleteRate(id);
        return ResponseEntity.ok(ApiResponse.ok("Rate deleted", null));
    }
}
