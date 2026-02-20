package com.gonerp.auth.controller;

import com.gonerp.auth.dto.AuthResponse;
import com.gonerp.auth.dto.LoginRequest;
import com.gonerp.common.ApiResponse;
import com.gonerp.config.JwtTokenProvider;
import com.gonerp.usermanager.model.User;
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

        AuthResponse response = AuthResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .userName(user.getUserName())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .avatarUrl(user.getAvatarUrl())
                .role(user.getRole().getName().name())
                .userId(user.getId())
                .build();

        return ResponseEntity.ok(ApiResponse.ok("Login successful", response));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<AuthResponse>> getCurrentUser(Authentication authentication) {
        User user = userRepository.findByUserName(authentication.getName()).orElseThrow();
        AuthResponse response = AuthResponse.builder()
                .userName(user.getUserName())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .avatarUrl(user.getAvatarUrl())
                .role(user.getRole().getName().name())
                .userId(user.getId())
                .build();
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
