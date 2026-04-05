package com.gonerp.ecommerce.controller;

import com.gonerp.common.ApiResponse;
import com.gonerp.ecommerce.dto.EcomDashboardResponse;
import com.gonerp.ecommerce.service.EcomDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/ecommerce/dashboard")
@RequiredArgsConstructor
public class EcomDashboardController {

    private final EcomDashboardService dashboardService;

    @GetMapping
    public ResponseEntity<ApiResponse<EcomDashboardResponse>> getDashboard(
            @RequestParam(required = false) Long storeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(ApiResponse.ok(dashboardService.getDashboard(storeId, startDate, endDate)));
    }
}
