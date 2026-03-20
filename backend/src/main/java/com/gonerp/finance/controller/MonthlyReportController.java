package com.gonerp.finance.controller;

import com.gonerp.common.ApiResponse;
import com.gonerp.finance.dto.MonthlyReportRequest;
import com.gonerp.finance.dto.MonthlyReportResponse;
import com.gonerp.finance.service.MonthlyReportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/finance/reports")
@RequiredArgsConstructor
public class MonthlyReportController {

    private final MonthlyReportService monthlyReportService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<MonthlyReportResponse>>> findAll() {
        return ResponseEntity.ok(ApiResponse.ok(monthlyReportService.findAll()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<MonthlyReportResponse>> findById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(monthlyReportService.findById(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<MonthlyReportResponse>> create(
            @Valid @RequestBody MonthlyReportRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Report created", monthlyReportService.create(request)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<MonthlyReportResponse>> update(
            @PathVariable Long id, @Valid @RequestBody MonthlyReportRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Report updated", monthlyReportService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        monthlyReportService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok("Report deleted", null));
    }
}
