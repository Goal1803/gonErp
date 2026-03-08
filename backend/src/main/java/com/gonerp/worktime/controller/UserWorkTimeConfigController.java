package com.gonerp.worktime.controller;

import com.gonerp.common.ApiResponse;
import com.gonerp.usermanager.model.User;
import com.gonerp.usermanager.repository.UserRepository;
import com.gonerp.worktime.dto.UserWorkTimeConfigRequest;
import com.gonerp.worktime.dto.UserWorkTimeConfigResponse;
import com.gonerp.worktime.service.UserWorkTimeConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/worktime/user-config")
@RequiredArgsConstructor
public class UserWorkTimeConfigController {

    private final UserWorkTimeConfigService configService;
    private final UserRepository userRepository;

    // Get my own config
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserWorkTimeConfigResponse>> getMyConfig(Authentication auth) {
        User user = userRepository.findByUserName(auth.getName()).orElseThrow();
        return ResponseEntity.ok(ApiResponse.ok(configService.getConfigResponse(user.getId())));
    }

    // Admin: get all user configs for the org
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<List<UserWorkTimeConfigResponse>>> getOrgConfigs(Authentication auth) {
        User user = userRepository.findByUserName(auth.getName()).orElseThrow();
        if (user.getOrganization() == null) {
            throw new IllegalStateException("User does not belong to an organization");
        }
        return ResponseEntity.ok(ApiResponse.ok(configService.getOrgConfigs(user.getOrganization().getId())));
    }

    // Admin: get a specific user's config
    @GetMapping("/{userId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<UserWorkTimeConfigResponse>> getUserConfig(@PathVariable Long userId) {
        return ResponseEntity.ok(ApiResponse.ok(configService.getConfigResponse(userId)));
    }

    // Admin: update a user's config
    @PutMapping("/{userId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<UserWorkTimeConfigResponse>> updateUserConfig(
            @PathVariable Long userId,
            @RequestBody UserWorkTimeConfigRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(configService.updateConfig(userId, request)));
    }
}
