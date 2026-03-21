package com.gonerp.finance.controller;

import com.gonerp.common.ApiResponse;
import com.gonerp.finance.dto.ReportExportResponse;
import com.gonerp.finance.model.FinanceReportExport;
import com.gonerp.finance.service.FinanceExportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/finance/export")
@RequiredArgsConstructor
public class ExportController {

    private final FinanceExportService exportService;

    @GetMapping("/excel/report/{reportId}")
    public ResponseEntity<byte[]> exportExcel(
            @PathVariable Long reportId,
            @RequestParam(required = false) Long accountId) {
        byte[] xlsx = exportService.exportReportExcel(reportId, accountId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=transactions.xlsx")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(xlsx);
    }

    @GetMapping("/zip/report/{reportId}")
    public ResponseEntity<byte[]> exportZip(@PathVariable Long reportId) {
        byte[] zip = exportService.exportReportZip(reportId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=monthly_report.zip")
                .contentType(MediaType.parseMediaType("application/zip"))
                .body(zip);
    }

    @PostMapping("/generate/{reportId}")
    public ResponseEntity<ApiResponse<ReportExportResponse>> generateReportZip(@PathVariable Long reportId) {
        FinanceReportExport export = exportService.generateReportZip(reportId);
        return ResponseEntity.ok(ApiResponse.ok(ReportExportResponse.from(export)));
    }

    @GetMapping("/history/{reportId}")
    public ResponseEntity<ApiResponse<List<ReportExportResponse>>> listExports(@PathVariable Long reportId) {
        List<ReportExportResponse> exports = exportService.listExports(reportId).stream()
                .map(ReportExportResponse::from)
                .toList();
        return ResponseEntity.ok(ApiResponse.ok(exports));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteExport(@PathVariable Long id) {
        exportService.deleteExport(id);
        return ResponseEntity.ok(ApiResponse.ok("Export deleted", null));
    }
}
