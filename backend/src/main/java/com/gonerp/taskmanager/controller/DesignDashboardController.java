package com.gonerp.taskmanager.controller;

import com.gonerp.common.ApiResponse;
import com.gonerp.common.OrgContext;
import com.gonerp.taskmanager.dto.DesignDashboardResponse;
import com.gonerp.taskmanager.model.BoardMember;
import com.gonerp.taskmanager.model.enums.BoardMemberRole;
import com.gonerp.taskmanager.repository.BoardMemberRepository;
import com.gonerp.taskmanager.repository.BoardRepository;
import com.gonerp.taskmanager.service.DesignDashboardService;
import com.gonerp.usermanager.model.User;
import com.gonerp.usermanager.model.enums.RoleName;
import com.gonerp.usermanager.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Optional;

@RestController
@RequestMapping("/api/tasks/dashboard")
@RequiredArgsConstructor
public class DesignDashboardController {

    private final DesignDashboardService designDashboardService;
    private final UserRepository userRepository;
    private final BoardMemberRepository boardMemberRepository;
    private final BoardRepository boardRepository;

    @GetMapping("/board/{boardId}")
    public ResponseEntity<ApiResponse<DesignDashboardResponse>> getBoardDashboard(
            @PathVariable Long boardId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long userId) {

        User currentUser = OrgContext.getCurrentUser(userRepository);
        boolean isAdmin = resolveFilterUserId(currentUser, boardId) == null;

        // Admin can filter by specific user; non-admin always sees only own data
        Long filterUserId;
        if (isAdmin) {
            filterUserId = userId; // null = all, or specific user
        } else {
            filterUserId = currentUser.getId();
        }

        DesignDashboardResponse response = designDashboardService.getDashboard(boardId, startDate, endDate, filterUserId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/combined")
    public ResponseEntity<ApiResponse<DesignDashboardResponse>> getCombinedDashboard(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long userId) {

        User currentUser = OrgContext.getCurrentUser(userRepository);
        Long orgId = currentUser.getOrganization() != null ? currentUser.getOrganization().getId() : null;

        if (orgId == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error("User has no organization"));
        }

        boolean isAdmin = isAdminUser(currentUser);
        Long filterUserId;
        if (isAdmin) {
            filterUserId = userId; // null = all, or specific user
        } else {
            filterUserId = currentUser.getId();
        }

        DesignDashboardResponse response = designDashboardService.getCombinedDashboard(startDate, endDate, filterUserId, orgId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    private Long resolveFilterUserId(User currentUser, Long boardId) {
        // Super admins see everything
        if (OrgContext.isSuperAdmin()) {
            return null;
        }

        // Check if user is the board owner
        var board = boardRepository.findById(boardId).orElse(null);
        if (board != null && board.getOwner().getId().equals(currentUser.getId())) {
            return null;
        }

        // Check board member role
        Optional<BoardMember> membership = boardMemberRepository.findByBoardIdAndUserId(boardId, currentUser.getId());
        if (membership.isPresent()) {
            BoardMemberRole role = membership.get().getRole();
            if (role == BoardMemberRole.OWNER || role == BoardMemberRole.ADMIN) {
                return null;
            }
        }

        // Regular member - filter to their own data
        return currentUser.getId();
    }

    private boolean isAdminUser(User user) {
        if (OrgContext.isSuperAdmin()) {
            return true;
        }
        // Check if user is ADMIN or OWNER on any POD_DESIGN board - simplified check
        // For combined view, we consider org-level admin status
        return user.getRole() != null &&
                (user.getRole().getName() == RoleName.ADMIN ||
                 user.getRole().getName() == RoleName.SUPER_ADMIN);
    }
}
