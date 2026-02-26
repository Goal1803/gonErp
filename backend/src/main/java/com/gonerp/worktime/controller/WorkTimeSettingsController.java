package com.gonerp.worktime.controller;

import com.gonerp.common.ApiResponse;
import com.gonerp.usermanager.model.User;
import com.gonerp.usermanager.repository.UserRepository;
import com.gonerp.worktime.dto.WorkTimeSettingsRequest;
import com.gonerp.worktime.dto.WorkTimeSettingsResponse;
import com.gonerp.worktime.service.WorkTimeSettingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/worktime/settings")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
public class WorkTimeSettingsController {

    private final WorkTimeSettingsService settingsService;
    private final UserRepository userRepository;

    @GetMapping
    public ResponseEntity<ApiResponse<WorkTimeSettingsResponse>> getSettings(Authentication auth) {
        User user = userRepository.findByUserName(auth.getName()).orElseThrow();
        Long orgId = user.getOrganization() != null ? user.getOrganization().getId() : null;
        if (orgId == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error("No organization"));
        }
        return ResponseEntity.ok(ApiResponse.ok(settingsService.getSettings(orgId)));
    }

    @PutMapping
    public ResponseEntity<ApiResponse<WorkTimeSettingsResponse>> updateSettings(
            Authentication auth, @RequestBody WorkTimeSettingsRequest request) {
        User user = userRepository.findByUserName(auth.getName()).orElseThrow();
        Long orgId = user.getOrganization() != null ? user.getOrganization().getId() : null;
        if (orgId == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error("No organization"));
        }
        return ResponseEntity.ok(ApiResponse.ok("Settings updated", settingsService.updateSettings(orgId, request)));
    }
}
