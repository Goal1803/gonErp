package com.gonerp.worktime.controller;

import com.gonerp.common.ApiResponse;
import com.gonerp.usermanager.model.User;
import com.gonerp.usermanager.repository.UserRepository;
import com.gonerp.worktime.dto.BulkQuotaAssignRequest;
import com.gonerp.worktime.dto.DayOffQuotaResponse;
import com.gonerp.worktime.dto.DayOffQuotaUpdateRequest;
import com.gonerp.worktime.service.DayOffQuotaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/worktime/quotas")
@RequiredArgsConstructor
public class DayOffQuotaController {

    private final DayOffQuotaService quotaService;
    private final UserRepository userRepository;

    @GetMapping("/my")
    public ResponseEntity<ApiResponse<List<DayOffQuotaResponse>>> getMyQuotas(
            Authentication auth,
            @RequestParam(required = false) Integer year) {
        User user = userRepository.findByUserName(auth.getName()).orElseThrow();
        int resolvedYear = year != null ? year : LocalDate.now().getYear();
        return ResponseEntity.ok(ApiResponse.ok(quotaService.getMyQuotas(user.getId(), resolvedYear)));
    }

    @GetMapping("/user/{userId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<List<DayOffQuotaResponse>>> getUserQuotas(
            @PathVariable Long userId,
            @RequestParam(required = false) Integer year) {
        int resolvedYear = year != null ? year : LocalDate.now().getYear();
        return ResponseEntity.ok(ApiResponse.ok(quotaService.getUserQuotas(userId, resolvedYear)));
    }

    @PutMapping("/user/{userId}/type/{typeId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<DayOffQuotaResponse>> setQuota(
            @PathVariable Long userId,
            @PathVariable Long typeId,
            @RequestBody DayOffQuotaUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Quota updated", quotaService.setQuota(userId, typeId, request)));
    }

    @PostMapping("/bulk-assign")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<List<DayOffQuotaResponse>>> bulkAssign(
            Authentication auth, @RequestBody BulkQuotaAssignRequest request) {
        User user = userRepository.findByUserName(auth.getName()).orElseThrow();
        Long orgId = user.getOrganization() != null ? user.getOrganization().getId() : null;
        if (orgId == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error("No organization"));
        }
        return ResponseEntity.ok(ApiResponse.ok("Quotas assigned", quotaService.bulkAssignDefaults(orgId, request)));
    }
}
