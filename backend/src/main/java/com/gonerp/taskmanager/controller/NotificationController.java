package com.gonerp.taskmanager.controller;

import com.gonerp.common.ApiResponse;
import com.gonerp.taskmanager.dto.NotificationResponse;
import com.gonerp.taskmanager.service.NotificationService;
import com.gonerp.usermanager.model.User;
import com.gonerp.usermanager.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final UserRepository userRepository;

    private User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUserName(username)
                .orElseThrow(() -> new RuntimeException("Current user not found"));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<NotificationResponse>>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Long userId = getCurrentUser().getId();
        return ResponseEntity.ok(ApiResponse.ok(notificationService.getNotifications(userId, page, size)));
    }

    @GetMapping("/important")
    public ResponseEntity<ApiResponse<Page<NotificationResponse>>> getImportant(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Long userId = getCurrentUser().getId();
        return ResponseEntity.ok(ApiResponse.ok(notificationService.getImportantNotifications(userId, page, size)));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getUnreadCount() {
        Long userId = getCurrentUser().getId();
        long count = notificationService.getUnreadCount(userId);
        return ResponseEntity.ok(ApiResponse.ok(Map.of("count", count)));
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<ApiResponse<Void>> markAsRead(@PathVariable Long id) {
        notificationService.markAsRead(id);
        return ResponseEntity.ok(ApiResponse.ok("Marked as read", null));
    }

    @PatchMapping("/read-all")
    public ResponseEntity<ApiResponse<Void>> markAllAsRead() {
        Long userId = getCurrentUser().getId();
        notificationService.markAllAsRead(userId);
        return ResponseEntity.ok(ApiResponse.ok("All marked as read", null));
    }
}
