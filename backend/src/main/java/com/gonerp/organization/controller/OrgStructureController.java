package com.gonerp.organization.controller;

import com.gonerp.common.ApiResponse;
import com.gonerp.organization.dto.OrgStructureRequest;
import com.gonerp.organization.dto.OrgStructureResponse;
import com.gonerp.organization.service.OrgStructureService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/org-structure")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
public class OrgStructureController {

    private final OrgStructureService orgStructureService;

    // ─── Staff Roles ─────────────────────────────────────────────────────

    @GetMapping("/staff-roles")
    public ResponseEntity<ApiResponse<List<OrgStructureResponse>>> getStaffRoles() {
        return ResponseEntity.ok(ApiResponse.ok(orgStructureService.getStaffRolesForCurrentOrg()));
    }

    @PostMapping("/staff-roles")
    public ResponseEntity<ApiResponse<OrgStructureResponse>> createStaffRole(
            @Valid @RequestBody OrgStructureRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(orgStructureService.createOrgStaffRole(request)));
    }

    @PutMapping("/staff-roles/{id}")
    public ResponseEntity<ApiResponse<OrgStructureResponse>> updateStaffRole(
            @PathVariable Long id, @Valid @RequestBody OrgStructureRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(orgStructureService.updateOrgStaffRole(id, request)));
    }

    @DeleteMapping("/staff-roles/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteStaffRole(@PathVariable Long id) {
        orgStructureService.deleteOrgStaffRole(id);
        return ResponseEntity.ok(ApiResponse.ok("Staff role deleted", null));
    }

    // ─── Departments ─────────────────────────────────────────────────────

    @GetMapping("/departments")
    public ResponseEntity<ApiResponse<List<OrgStructureResponse>>> getDepartments() {
        return ResponseEntity.ok(ApiResponse.ok(orgStructureService.getDepartmentsForCurrentOrg()));
    }

    @PostMapping("/departments")
    public ResponseEntity<ApiResponse<OrgStructureResponse>> createDepartment(
            @Valid @RequestBody OrgStructureRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(orgStructureService.createOrgDepartment(request)));
    }

    @PutMapping("/departments/{id}")
    public ResponseEntity<ApiResponse<OrgStructureResponse>> updateDepartment(
            @PathVariable Long id, @Valid @RequestBody OrgStructureRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(orgStructureService.updateOrgDepartment(id, request)));
    }

    @DeleteMapping("/departments/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteDepartment(@PathVariable Long id) {
        orgStructureService.deleteOrgDepartment(id);
        return ResponseEntity.ok(ApiResponse.ok("Department deleted", null));
    }

    // ─── User Groups ─────────────────────────────────────────────────────

    @GetMapping("/user-groups")
    public ResponseEntity<ApiResponse<List<OrgStructureResponse>>> getUserGroups() {
        return ResponseEntity.ok(ApiResponse.ok(orgStructureService.getUserGroupsForCurrentOrg()));
    }

    @PostMapping("/user-groups")
    public ResponseEntity<ApiResponse<OrgStructureResponse>> createUserGroup(
            @Valid @RequestBody OrgStructureRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(orgStructureService.createOrgUserGroup(request)));
    }

    @PutMapping("/user-groups/{id}")
    public ResponseEntity<ApiResponse<OrgStructureResponse>> updateUserGroup(
            @PathVariable Long id, @Valid @RequestBody OrgStructureRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(orgStructureService.updateOrgUserGroup(id, request)));
    }

    @DeleteMapping("/user-groups/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteUserGroup(@PathVariable Long id) {
        orgStructureService.deleteOrgUserGroup(id);
        return ResponseEntity.ok(ApiResponse.ok("User group deleted", null));
    }

    // ─── User Assignments ────────────────────────────────────────────────

    @PostMapping("/users/{userId}/staff-roles/{roleId}")
    public ResponseEntity<ApiResponse<Void>> assignStaffRole(
            @PathVariable Long userId, @PathVariable Long roleId) {
        orgStructureService.assignStaffRole(userId, roleId);
        return ResponseEntity.ok(ApiResponse.ok("Staff role assigned", null));
    }

    @DeleteMapping("/users/{userId}/staff-roles/{roleId}")
    public ResponseEntity<ApiResponse<Void>> removeStaffRole(
            @PathVariable Long userId, @PathVariable Long roleId) {
        orgStructureService.removeStaffRole(userId, roleId);
        return ResponseEntity.ok(ApiResponse.ok("Staff role removed", null));
    }

    @PostMapping("/users/{userId}/departments/{deptId}")
    public ResponseEntity<ApiResponse<Void>> assignDepartment(
            @PathVariable Long userId, @PathVariable Long deptId) {
        orgStructureService.assignDepartment(userId, deptId);
        return ResponseEntity.ok(ApiResponse.ok("Department assigned", null));
    }

    @DeleteMapping("/users/{userId}/departments/{deptId}")
    public ResponseEntity<ApiResponse<Void>> removeDepartment(
            @PathVariable Long userId, @PathVariable Long deptId) {
        orgStructureService.removeDepartment(userId, deptId);
        return ResponseEntity.ok(ApiResponse.ok("Department removed", null));
    }

    @PostMapping("/users/{userId}/user-groups/{groupId}")
    public ResponseEntity<ApiResponse<Void>> assignUserGroup(
            @PathVariable Long userId, @PathVariable Long groupId) {
        orgStructureService.assignUserGroup(userId, groupId);
        return ResponseEntity.ok(ApiResponse.ok("User group assigned", null));
    }

    @DeleteMapping("/users/{userId}/user-groups/{groupId}")
    public ResponseEntity<ApiResponse<Void>> removeUserGroup(
            @PathVariable Long userId, @PathVariable Long groupId) {
        orgStructureService.removeUserGroup(userId, groupId);
        return ResponseEntity.ok(ApiResponse.ok("User group removed", null));
    }
}
