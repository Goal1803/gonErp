package com.gonerp.taskmanager.controller;

import com.gonerp.common.ApiResponse;
import com.gonerp.organization.dto.OrgStructureRequest;
import com.gonerp.organization.dto.OrgStructureResponse;
import com.gonerp.taskmanager.dto.DesignUserAssignmentResponse;
import com.gonerp.taskmanager.service.DesignConfigService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tasks/designs/config")
@RequiredArgsConstructor
public class DesignConfigController {

    private final DesignConfigService designConfigService;

    // ─── Staff Roles (SUPER_ADMIN) ───────────────────────────────────────

    @GetMapping("/staff-roles")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<List<OrgStructureResponse>>> getStaffRoles() {
        return ResponseEntity.ok(ApiResponse.ok(designConfigService.getAllStaffRoles()));
    }

    @PostMapping("/staff-roles")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<OrgStructureResponse>> createStaffRole(
            @Valid @RequestBody OrgStructureRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(designConfigService.createStaffRole(request)));
    }

    @PutMapping("/staff-roles/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<OrgStructureResponse>> updateStaffRole(
            @PathVariable Long id, @Valid @RequestBody OrgStructureRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(designConfigService.updateStaffRole(id, request)));
    }

    @DeleteMapping("/staff-roles/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteStaffRole(@PathVariable Long id) {
        designConfigService.deleteStaffRole(id);
        return ResponseEntity.ok(ApiResponse.ok("Design staff role deleted", null));
    }

    // ─── User Groups (SUPER_ADMIN) ───────────────────────────────────────

    @GetMapping("/user-groups")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<List<OrgStructureResponse>>> getUserGroups() {
        return ResponseEntity.ok(ApiResponse.ok(designConfigService.getAllUserGroups()));
    }

    @PostMapping("/user-groups")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<OrgStructureResponse>> createUserGroup(
            @Valid @RequestBody OrgStructureRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(designConfigService.createUserGroup(request)));
    }

    @PutMapping("/user-groups/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<OrgStructureResponse>> updateUserGroup(
            @PathVariable Long id, @Valid @RequestBody OrgStructureRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(designConfigService.updateUserGroup(id, request)));
    }

    @DeleteMapping("/user-groups/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteUserGroup(@PathVariable Long id) {
        designConfigService.deleteUserGroup(id);
        return ResponseEntity.ok(ApiResponse.ok("Design user group deleted", null));
    }

    // ─── User Assignments (ADMIN + SUPER_ADMIN) ─────────────────────────

    @GetMapping("/users")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<List<DesignUserAssignmentResponse>>> getUsers() {
        return ResponseEntity.ok(ApiResponse.ok(designConfigService.getOrgUsersWithAssignments()));
    }

    @PostMapping("/users/{userId}/staff-roles/{roleId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> assignStaffRole(
            @PathVariable Long userId, @PathVariable Long roleId) {
        designConfigService.assignStaffRole(userId, roleId);
        return ResponseEntity.ok(ApiResponse.ok("Design staff role assigned", null));
    }

    @DeleteMapping("/users/{userId}/staff-roles/{roleId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> removeStaffRole(
            @PathVariable Long userId, @PathVariable Long roleId) {
        designConfigService.removeStaffRole(userId, roleId);
        return ResponseEntity.ok(ApiResponse.ok("Design staff role removed", null));
    }

    @PostMapping("/users/{userId}/user-groups/{groupId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> assignUserGroup(
            @PathVariable Long userId, @PathVariable Long groupId) {
        designConfigService.assignUserGroup(userId, groupId);
        return ResponseEntity.ok(ApiResponse.ok("Design user group assigned", null));
    }

    @DeleteMapping("/users/{userId}/user-groups/{groupId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> removeUserGroup(
            @PathVariable Long userId, @PathVariable Long groupId) {
        designConfigService.removeUserGroup(userId, groupId);
        return ResponseEntity.ok(ApiResponse.ok("Design user group removed", null));
    }
}
