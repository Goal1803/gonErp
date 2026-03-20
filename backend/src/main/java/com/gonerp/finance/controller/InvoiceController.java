package com.gonerp.finance.controller;

import com.gonerp.common.ApiResponse;
import com.gonerp.finance.dto.InvoiceResponse;
import com.gonerp.finance.service.InvoiceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/finance/invoices")
@RequiredArgsConstructor
public class InvoiceController {

    private final InvoiceService invoiceService;

    @GetMapping("/report/{reportId}")
    public ResponseEntity<ApiResponse<List<InvoiceResponse>>> findByReport(@PathVariable Long reportId) {
        return ResponseEntity.ok(ApiResponse.ok(invoiceService.findByReport(reportId)));
    }

    @GetMapping("/transaction/{transactionId}")
    public ResponseEntity<ApiResponse<List<InvoiceResponse>>> findByTransaction(@PathVariable Long transactionId) {
        return ResponseEntity.ok(ApiResponse.ok(invoiceService.findByTransaction(transactionId)));
    }

    @PostMapping("/upload")
    public ResponseEntity<ApiResponse<InvoiceResponse>> upload(
            @RequestParam Long reportId,
            @RequestParam(required = false) Long transactionId,
            @RequestParam(required = false) String invoiceType,
            @RequestParam(required = false) String description,
            @RequestParam("file") MultipartFile file) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Invoice uploaded", invoiceService.upload(reportId, transactionId, invoiceType, description, file)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        invoiceService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok("Invoice deleted", null));
    }
}
