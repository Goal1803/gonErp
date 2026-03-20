package com.gonerp.finance.controller;

import com.gonerp.finance.service.FinanceExportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/finance/export")
@RequiredArgsConstructor
public class ExportController {

    private final FinanceExportService exportService;

    @GetMapping("/csv/report/{reportId}")
    public ResponseEntity<byte[]> exportCsv(
            @PathVariable Long reportId,
            @RequestParam(required = false) Long accountId) {
        byte[] csv = exportService.exportReportCsv(reportId, accountId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=transactions.csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv);
    }

    @GetMapping("/zip/report/{reportId}")
    public ResponseEntity<byte[]> exportZip(@PathVariable Long reportId) {
        byte[] zip = exportService.exportReportZip(reportId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=monthly_report.zip")
                .contentType(MediaType.parseMediaType("application/zip"))
                .body(zip);
    }
}
