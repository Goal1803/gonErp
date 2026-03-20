package com.gonerp.finance.controller;

import com.gonerp.common.ApiResponse;
import com.gonerp.finance.dto.ShareLinkRequest;
import com.gonerp.finance.dto.ShareLinkResponse;
import com.gonerp.finance.service.ShareLinkService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/finance/share-links")
@RequiredArgsConstructor
public class ShareLinkController {

    private final ShareLinkService shareLinkService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<ShareLinkResponse>>> findAll() {
        return ResponseEntity.ok(ApiResponse.ok(shareLinkService.findByOrg()));
    }

    @GetMapping("/report/{reportId}")
    public ResponseEntity<ApiResponse<List<ShareLinkResponse>>> findByReport(@PathVariable Long reportId) {
        return ResponseEntity.ok(ApiResponse.ok(shareLinkService.findByReport(reportId)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ShareLinkResponse>> create(@Valid @RequestBody ShareLinkRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Share link created", shareLinkService.create(request)));
    }

    @PatchMapping("/{id}/toggle")
    public ResponseEntity<ApiResponse<ShareLinkResponse>> toggleActive(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(shareLinkService.toggleActive(id)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        shareLinkService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok("Share link deleted", null));
    }
}
