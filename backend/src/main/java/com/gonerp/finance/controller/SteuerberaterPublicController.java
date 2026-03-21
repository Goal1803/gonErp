package com.gonerp.finance.controller;

import com.gonerp.common.ApiResponse;
import com.gonerp.finance.dto.*;
import com.gonerp.finance.service.FinanceExportService;
import com.gonerp.finance.service.SteuerberaterService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/finance/public")
@RequiredArgsConstructor
public class SteuerberaterPublicController {

    private final SteuerberaterService steuerberaterService;
    private final FinanceExportService exportService;

    @GetMapping("/{token}/report")
    public ResponseEntity<ApiResponse<MonthlyReportResponse>> getReport(@PathVariable String token) {
        return ResponseEntity.ok(ApiResponse.ok(steuerberaterService.getReport(token)));
    }

    @GetMapping("/{token}/accounts")
    public ResponseEntity<ApiResponse<List<FinanceAccountResponse>>> getAccounts(@PathVariable String token) {
        return ResponseEntity.ok(ApiResponse.ok(steuerberaterService.getAccounts(token)));
    }

    @GetMapping("/{token}/transactions")
    public ResponseEntity<ApiResponse<List<TransactionResponse>>> getTransactions(
            @PathVariable String token,
            @RequestParam(required = false) Long accountId) {
        return ResponseEntity.ok(ApiResponse.ok(steuerberaterService.getTransactions(token, accountId)));
    }

    @GetMapping("/{token}/invoices")
    public ResponseEntity<ApiResponse<List<InvoiceResponse>>> getInvoices(@PathVariable String token) {
        return ResponseEntity.ok(ApiResponse.ok(steuerberaterService.getInvoices(token)));
    }

    @GetMapping("/{token}/comments")
    public ResponseEntity<ApiResponse<List<SteuerberaterCommentResponse>>> getComments(@PathVariable String token) {
        return ResponseEntity.ok(ApiResponse.ok(steuerberaterService.getComments(token)));
    }

    @PostMapping("/{token}/comments")
    public ResponseEntity<ApiResponse<SteuerberaterCommentResponse>> addComment(
            @PathVariable String token,
            @Valid @RequestBody SteuerberaterCommentRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Comment added", steuerberaterService.addComment(token, request)));
    }

    @GetMapping("/{token}/export/excel")
    public ResponseEntity<byte[]> exportExcel(
            @PathVariable String token,
            @RequestParam(required = false) Long accountId) {
        var link = steuerberaterService.validateAndAccessLink(token);
        byte[] xlsx = exportService.exportReportExcel(link.getMonthlyReport().getId(), accountId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=transactions.xlsx")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(xlsx);
    }
}
