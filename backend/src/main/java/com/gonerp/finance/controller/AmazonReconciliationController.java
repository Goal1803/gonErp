package com.gonerp.finance.controller;

import com.gonerp.common.ApiResponse;
import com.gonerp.finance.dto.AmazonReconciliationResponse;
import com.gonerp.finance.service.AmazonReconciliationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/finance/amazon-reconciliation")
@RequiredArgsConstructor
public class AmazonReconciliationController {

    private final AmazonReconciliationService reconciliationService;

    @GetMapping("/report/{reportId}")
    public ResponseEntity<ApiResponse<List<AmazonReconciliationResponse>>> findByReport(@PathVariable Long reportId) {
        return ResponseEntity.ok(ApiResponse.ok(reconciliationService.findByReport(reportId)));
    }

    @PostMapping("/generate/{reportId}")
    public ResponseEntity<ApiResponse<AmazonReconciliationResponse>> generate(
            @PathVariable Long reportId,
            @RequestParam(required = false) String marketplace) {
        return ResponseEntity.ok(ApiResponse.ok("Reconciliation generated", reconciliationService.generate(reportId, marketplace)));
    }
}
