package com.gonerp.auth.controller;

import com.gonerp.auth.dto.AuthResponse;
import com.gonerp.auth.dto.LoginRequest;
import com.gonerp.common.ApiResponse;
import com.gonerp.config.JwtTokenProvider;
import com.gonerp.organization.model.Organization;
import com.gonerp.usermanager.model.User;
import com.gonerp.usermanager.model.enums.RoleName;
import com.gonerp.usermanager.repository.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUserName(), request.getPassword())
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);
        String token = jwtTokenProvider.generateToken(authentication);

        User user = userRepository.findByUserName(request.getUserName()).orElseThrow();

        // Check org active (except SUPER_ADMIN)
        boolean isSuperAdmin = user.getRole().getName() == RoleName.SUPER_ADMIN;
        if (!isSuperAdmin && user.getOrganization() != null && !user.getOrganization().isActive()) {
            return ResponseEntity.status(403)
                    .body(ApiResponse.error("Your organization has been deactivated"));
        }

        return ResponseEntity.ok(ApiResponse.ok("Login successful", buildAuthResponse(user, token)));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<AuthResponse>> getCurrentUser(Authentication authentication) {
        User user = userRepository.findByUserName(authentication.getName()).orElseThrow();
        return ResponseEntity.ok(ApiResponse.ok(buildAuthResponse(user, null)));
    }

    private AuthResponse buildAuthResponse(User user, String token) {
        boolean isSuperAdmin = user.getRole().getName() == RoleName.SUPER_ADMIN;
        Organization org = user.getOrganization();

        AuthResponse.AuthResponseBuilder builder = AuthResponse.builder()
                .userName(user.getUserName())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .avatarUrl(user.getAvatarUrl())
                .role(user.getRole().getName().name())
                .userId(user.getId())
                .designsManager(user.isDesignsManager());

        if (token != null) {
            builder.token(token).tokenType("Bearer");
        }

        if (isSuperAdmin) {
            builder.moduleTaskManager(true)
                    .moduleImageManager(true)
                    .moduleDesigns(true);
        } else if (org != null) {
            builder.organizationId(org.getId())
                    .organizationName(org.getName())
                    .organizationSlug(org.getSlug())
                    .moduleTaskManager(org.isModuleTaskManager())
                    .moduleImageManager(org.isModuleImageManager())
                    .moduleDesigns(org.isModuleDesigns());
        }

        return builder.build();
    }
}
