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
}
