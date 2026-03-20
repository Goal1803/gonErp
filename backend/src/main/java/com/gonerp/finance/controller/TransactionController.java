package com.gonerp.finance.controller;

import com.gonerp.common.ApiResponse;
import com.gonerp.finance.dto.TransactionFileResponse;
import com.gonerp.finance.dto.TransactionResponse;
import com.gonerp.finance.dto.TransactionUpdateRequest;
import com.gonerp.finance.service.TransactionFileService;
import com.gonerp.finance.service.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/finance/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;
    private final TransactionFileService transactionFileService;

    @GetMapping("/report/{reportId}")
    public ResponseEntity<ApiResponse<List<TransactionResponse>>> findByReport(@PathVariable Long reportId) {
        return ResponseEntity.ok(ApiResponse.ok(transactionService.findByReport(reportId)));
    }

    @GetMapping("/report/{reportId}/account/{accountId}")
    public ResponseEntity<ApiResponse<List<TransactionResponse>>> findByReportAndAccount(
            @PathVariable Long reportId, @PathVariable Long accountId) {
        return ResponseEntity.ok(ApiResponse.ok(transactionService.findByReportAndAccount(reportId, accountId)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<TransactionResponse>> update(
            @PathVariable Long id, @RequestBody TransactionUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Transaction updated", transactionService.update(id, request)));
    }

    @PatchMapping("/{id}/toggle-completed")
    public ResponseEntity<ApiResponse<TransactionResponse>> toggleCompleted(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(transactionService.toggleCompleted(id)));
    }

    @PostMapping("/{id}/reset")
    public ResponseEntity<ApiResponse<TransactionResponse>> resetOne(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok("Transaction reset", transactionService.resetOne(id)));
    }

    @PostMapping("/reset-all/report/{reportId}")
    public ResponseEntity<ApiResponse<Integer>> resetAll(@PathVariable Long reportId) {
        int count = transactionService.resetAllForReport(reportId);
        return ResponseEntity.ok(ApiResponse.ok("Reset " + count + " transactions", count));
    }

    @PostMapping("/re-extract/report/{reportId}")
    public ResponseEntity<ApiResponse<Integer>> reExtract(@PathVariable Long reportId) {
        int count = transactionFileService.reExtractReport(reportId);
        return ResponseEntity.ok(ApiResponse.ok("Re-extracted " + count + " transactions", count));
    }

    // --- File uploads ---

    @GetMapping("/files/report/{reportId}")
    public ResponseEntity<ApiResponse<List<TransactionFileResponse>>> findFilesByReport(@PathVariable Long reportId) {
        return ResponseEntity.ok(ApiResponse.ok(transactionFileService.findByReport(reportId)));
    }

    @PostMapping("/files/upload")
    public ResponseEntity<ApiResponse<TransactionFileResponse>> uploadFile(
            @RequestParam Long reportId,
            @RequestParam Long accountId,
            @RequestParam(defaultValue = "TRANSACTION") String fileType,
            @RequestParam(required = false) String subfolder,
            @RequestParam("file") MultipartFile file) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("File uploaded", transactionFileService.upload(reportId, accountId, fileType, subfolder, file)));
    }

    @PatchMapping("/files/{fileId}/move")
    public ResponseEntity<ApiResponse<TransactionFileResponse>> moveFile(
            @PathVariable Long fileId,
            @RequestParam(required = false) String subfolder) {
        return ResponseEntity.ok(ApiResponse.ok("File moved", transactionFileService.moveToSubfolder(fileId, subfolder)));
    }

    @PatchMapping("/files/rename-folder")
    public ResponseEntity<ApiResponse<Integer>> renameFolder(
            @RequestParam Long reportId,
            @RequestParam Long accountId,
            @RequestParam String oldName,
            @RequestParam String newName) {
        int count = transactionFileService.renameSubfolder(reportId, accountId, oldName, newName);
        return ResponseEntity.ok(ApiResponse.ok("Renamed " + count + " files", count));
    }

    @DeleteMapping("/files/{fileId}")
    public ResponseEntity<ApiResponse<Void>> deleteFile(@PathVariable Long fileId) {
        transactionFileService.delete(fileId);
        return ResponseEntity.ok(ApiResponse.ok("File and transactions deleted", null));
    }
}
