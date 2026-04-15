package com.gonerp.worktime.controller;

import com.gonerp.common.ApiResponse;
import com.gonerp.usermanager.model.User;
import com.gonerp.usermanager.repository.UserRepository;
import com.gonerp.worktime.dto.DayOffRequestCreateDTO;
import com.gonerp.worktime.dto.DayOffRequestResponse;
import com.gonerp.worktime.service.DayOffRequestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/worktime/day-off-requests")
@RequiredArgsConstructor
public class DayOffRequestController {

    private final DayOffRequestService requestService;
    private final UserRepository userRepository;

    @GetMapping("/my")
    public ResponseEntity<ApiResponse<List<DayOffRequestResponse>>> getMyRequests(Authentication auth) {
        User user = userRepository.findByUserName(auth.getName()).orElseThrow();
        return ResponseEntity.ok(ApiResponse.ok(requestService.getMyRequests(user.getId())));
    }

    @GetMapping("/preview-days")
    public ResponseEntity<ApiResponse<Map<String, Double>>> previewDays(
            Authentication auth,
            @RequestParam("startDate") String startDate,
            @RequestParam("endDate") String endDate,
            @RequestParam(value = "halfDay", required = false) String halfDay) {
        User user = userRepository.findByUserName(auth.getName()).orElseThrow();
        double days = requestService.previewTotalDays(user.getId(),
                java.time.LocalDate.parse(startDate), java.time.LocalDate.parse(endDate), halfDay);
        return ResponseEntity.ok(ApiResponse.ok(Map.of("days", days)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<DayOffRequestResponse>> createRequest(
            Authentication auth, @Valid @RequestBody DayOffRequestCreateDTO dto) {
        User user = userRepository.findByUserName(auth.getName()).orElseThrow();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Day-off request created", requestService.createRequest(user.getId(), dto)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<DayOffRequestResponse>> cancelRequest(
            Authentication auth, @PathVariable Long id) {
        User user = userRepository.findByUserName(auth.getName()).orElseThrow();
        return ResponseEntity.ok(ApiResponse.ok("Request cancelled", requestService.cancelRequest(id, user.getId())));
    }

    @GetMapping("/pending")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<List<DayOffRequestResponse>>> getPendingRequests(Authentication auth) {
        User user = userRepository.findByUserName(auth.getName()).orElseThrow();
        Long orgId = user.getOrganization() != null ? user.getOrganization().getId() : null;
        if (orgId == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error("No organization"));
        }
        return ResponseEntity.ok(ApiResponse.ok(requestService.getPendingRequests(orgId)));
    }

    @PatchMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<DayOffRequestResponse>> approve(
            Authentication auth, @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> body) {
        User user = userRepository.findByUserName(auth.getName()).orElseThrow();
        String comment = body != null ? body.get("comment") : null;
        return ResponseEntity.ok(ApiResponse.ok("Request approved", requestService.approve(id, user.getId(), comment)));
    }

    @PatchMapping("/{id}/deny")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<DayOffRequestResponse>> deny(
            Authentication auth, @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> body) {
        User user = userRepository.findByUserName(auth.getName()).orElseThrow();
        String comment = body != null ? body.get("comment") : null;
        return ResponseEntity.ok(ApiResponse.ok("Request denied", requestService.deny(id, user.getId(), comment)));
    }

    @PostMapping("/bulk-approve")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<List<DayOffRequestResponse>>> bulkApprove(
            Authentication auth, @RequestBody Map<String, Object> body) {
        User user = userRepository.findByUserName(auth.getName()).orElseThrow();
        @SuppressWarnings("unchecked")
        List<Integer> rawIds = (List<Integer>) body.get("ids");
        List<Long> ids = rawIds == null ? List.of() : rawIds.stream().map(Integer::longValue).toList();
        String comment = (String) body.get("comment");
        return ResponseEntity.ok(ApiResponse.ok("Bulk approved", requestService.bulkApprove(ids, user.getId(), comment)));
    }

    @PostMapping("/bulk-deny")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<List<DayOffRequestResponse>>> bulkDeny(
            Authentication auth, @RequestBody Map<String, Object> body) {
        User user = userRepository.findByUserName(auth.getName()).orElseThrow();
        @SuppressWarnings("unchecked")
        List<Integer> rawIds = (List<Integer>) body.get("ids");
        List<Long> ids = rawIds == null ? List.of() : rawIds.stream().map(Integer::longValue).toList();
        String comment = (String) body.get("comment");
        return ResponseEntity.ok(ApiResponse.ok("Bulk denied", requestService.bulkDeny(ids, user.getId(), comment)));
    }

    @PatchMapping("/{id}/revoke")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<DayOffRequestResponse>> revoke(
            Authentication auth, @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> body) {
        User user = userRepository.findByUserName(auth.getName()).orElseThrow();
        String comment = body != null ? body.get("comment") : null;
        return ResponseEntity.ok(ApiResponse.ok("Request revoked", requestService.adminRevoke(id, user.getId(), comment)));
    }

    @GetMapping("/admin")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<List<DayOffRequestResponse>>> adminList(
            Authentication auth,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Long typeId,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to) {
        User user = userRepository.findByUserName(auth.getName()).orElseThrow();
        Long orgId = user.getOrganization() != null ? user.getOrganization().getId() : null;
        if (orgId == null) return ResponseEntity.badRequest().body(ApiResponse.error("No organization"));
        java.time.LocalDate fromDate = (from != null && !from.isBlank()) ? java.time.LocalDate.parse(from) : null;
        java.time.LocalDate toDate = (to != null && !to.isBlank()) ? java.time.LocalDate.parse(to) : null;
        return ResponseEntity.ok(ApiResponse.ok(
                requestService.adminList(orgId, status, userId, typeId, fromDate, toDate)));
    }

    @GetMapping("/admin/report")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> reportSummary(
            Authentication auth,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to) {
        User user = userRepository.findByUserName(auth.getName()).orElseThrow();
        Long orgId = user.getOrganization() != null ? user.getOrganization().getId() : null;
        if (orgId == null) return ResponseEntity.badRequest().body(ApiResponse.error("No organization"));
        java.time.LocalDate fromDate = (from != null && !from.isBlank()) ? java.time.LocalDate.parse(from) : null;
        java.time.LocalDate toDate = (to != null && !to.isBlank()) ? java.time.LocalDate.parse(to) : null;
        return ResponseEntity.ok(ApiResponse.ok(requestService.reportSummary(orgId, fromDate, toDate)));
    }

    @GetMapping("/admin/export")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<byte[]> exportCsv(
            Authentication auth,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Long typeId,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to) {
        User user = userRepository.findByUserName(auth.getName()).orElseThrow();
        Long orgId = user.getOrganization() != null ? user.getOrganization().getId() : null;
        if (orgId == null) return ResponseEntity.badRequest().build();
        java.time.LocalDate fromDate = (from != null && !from.isBlank()) ? java.time.LocalDate.parse(from) : null;
        java.time.LocalDate toDate = (to != null && !to.isBlank()) ? java.time.LocalDate.parse(to) : null;
        List<DayOffRequestResponse> rows = requestService.adminList(orgId, status, userId, typeId, fromDate, toDate);

        StringBuilder sb = new StringBuilder();
        sb.append("ID,User,Type,Start,End,HalfDay,Days,Status,Reason,Reviewer,ReviewedAt,ReviewComment,CreatedAt\n");
        for (DayOffRequestResponse r : rows) {
            sb.append(r.getId()).append(',')
              .append(csv(r.getUserName())).append(',')
              .append(csv(r.getDayOffTypeName())).append(',')
              .append(r.getStartDate()).append(',')
              .append(r.getEndDate()).append(',')
              .append(csv(r.getHalfDayType())).append(',')
              .append(r.getTotalDays()).append(',')
              .append(csv(r.getStatus())).append(',')
              .append(csv(r.getReason())).append(',')
              .append(csv(r.getReviewedByName())).append(',')
              .append(r.getReviewedAt() != null ? r.getReviewedAt() : "").append(',')
              .append(csv(r.getReviewComment())).append(',')
              .append(r.getCreatedAt() != null ? r.getCreatedAt() : "").append('\n');
        }
        byte[] bytes = sb.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(org.springframework.http.HttpHeaders.CONTENT_TYPE, "text/csv; charset=UTF-8")
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"day-off-requests.csv\"")
                .body(bytes);
    }

    private static String csv(String s) {
        if (s == null) return "";
        String v = s.replace("\"", "\"\"");
        if (v.contains(",") || v.contains("\n") || v.contains("\"")) return "\"" + v + "\"";
        return v;
    }
}
