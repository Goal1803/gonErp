package com.gonerp.finance.controller;

import com.gonerp.common.ApiResponse;
import com.gonerp.finance.dto.CategorizationRuleRequest;
import com.gonerp.finance.dto.CategorizationRuleResponse;
import com.gonerp.finance.service.CategorizationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/finance/rules")
@RequiredArgsConstructor
public class CategorizationRuleController {

    private final CategorizationService categorizationService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<CategorizationRuleResponse>>> findAll() {
        return ResponseEntity.ok(ApiResponse.ok(categorizationService.findAllRules()));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<CategorizationRuleResponse>> create(
            @Valid @RequestBody CategorizationRuleRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Rule created", categorizationService.createRule(request)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<CategorizationRuleResponse>> update(
            @PathVariable Long id, @Valid @RequestBody CategorizationRuleRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Rule updated", categorizationService.updateRule(id, request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        categorizationService.deleteRule(id);
        return ResponseEntity.ok(ApiResponse.ok("Rule deleted", null));
    }

    @PostMapping("/categorize/{reportId}")
    public ResponseEntity<ApiResponse<Integer>> categorizeReport(@PathVariable Long reportId) {
        int count = categorizationService.categorizeReport(reportId);
        return ResponseEntity.ok(ApiResponse.ok("Categorized " + count + " transactions", count));
    }

    @PostMapping("/seed-defaults")
    public ResponseEntity<ApiResponse<Integer>> seedDefaults() {
        int count = categorizationService.seedDefaultRules();
        return ResponseEntity.ok(ApiResponse.ok("Seeded " + count + " default rules", count));
    }
}
