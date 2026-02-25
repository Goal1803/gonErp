package com.gonerp.auth.controller;

import com.gonerp.auth.dto.ChangePasswordRequest;
import com.gonerp.auth.dto.ProfileUpdateRequest;
import com.gonerp.common.ApiResponse;
import com.gonerp.usermanager.dto.UserResponse;
import com.gonerp.usermanager.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final UserService userService;

    @GetMapping
    public ResponseEntity<ApiResponse<UserResponse>> getProfile(Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.ok(userService.getProfile(authentication.getName())));
    }

    @PutMapping
    public ResponseEntity<ApiResponse<UserResponse>> updateProfile(
            Authentication authentication,
            @Valid @RequestBody ProfileUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Profile updated successfully",
                userService.updateProfile(authentication.getName(), request)));
    }

    @PostMapping("/avatar")
    public ResponseEntity<ApiResponse<UserResponse>> uploadAvatar(
            Authentication authentication,
            @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(ApiResponse.ok("Avatar uploaded successfully",
                userService.uploadProfileAvatar(authentication.getName(), file)));
    }

    @DeleteMapping("/avatar")
    public ResponseEntity<ApiResponse<UserResponse>> deleteAvatar(Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.ok("Avatar removed successfully",
                userService.deleteProfileAvatar(authentication.getName())));
    }

    @PutMapping("/password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            Authentication authentication,
            @Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(authentication.getName(), request);
        return ResponseEntity.ok(ApiResponse.ok("Password changed successfully", null));
    }
}
