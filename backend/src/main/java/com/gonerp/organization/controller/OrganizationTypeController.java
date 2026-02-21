package com.gonerp.organization.controller;

import com.gonerp.common.ApiResponse;
import com.gonerp.organization.dto.OrgStructureRequest;
import com.gonerp.organization.dto.OrgStructureResponse;
import com.gonerp.organization.dto.OrganizationTypeRequest;
import com.gonerp.organization.dto.OrganizationTypeResponse;
import com.gonerp.organization.service.OrgStructureService;
import com.gonerp.organization.service.OrganizationTypeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/organization-types")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class OrganizationTypeController {

    private final OrganizationTypeService orgTypeService;
    private final OrgStructureService orgStructureService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<OrganizationTypeResponse>>> findAll() {
        return ResponseEntity.ok(ApiResponse.ok(orgTypeService.findAll()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<OrganizationTypeResponse>> findById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(orgTypeService.findById(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<OrganizationTypeResponse>> create(
            @Valid @RequestBody OrganizationTypeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Organization type created", orgTypeService.create(request)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<OrganizationTypeResponse>> update(
            @PathVariable Long id, @Valid @RequestBody OrganizationTypeRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Organization type updated", orgTypeService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        orgTypeService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok("Organization type deleted", null));
    }

    // ─── Default staff roles for this org type ───────────────────────────

    @GetMapping("/{id}/staff-roles")
    public ResponseEntity<ApiResponse<List<OrgStructureResponse>>> getStaffRoles(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(orgStructureService.getDefaultStaffRoles(id)));
    }

    @PostMapping("/{id}/staff-roles")
    public ResponseEntity<ApiResponse<OrgStructureResponse>> createStaffRole(
            @PathVariable Long id, @Valid @RequestBody OrgStructureRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(orgStructureService.createDefaultStaffRole(id, request)));
    }

    @DeleteMapping("/staff-roles/{roleId}")
    public ResponseEntity<ApiResponse<Void>> deleteStaffRole(@PathVariable Long roleId) {
        orgStructureService.deleteDefaultStaffRole(roleId);
        return ResponseEntity.ok(ApiResponse.ok("Staff role deleted", null));
    }

    // ─── Default departments for this org type ───────────────────────────

    @GetMapping("/{id}/departments")
    public ResponseEntity<ApiResponse<List<OrgStructureResponse>>> getDepartments(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(orgStructureService.getDefaultDepartments(id)));
    }

    @PostMapping("/{id}/departments")
    public ResponseEntity<ApiResponse<OrgStructureResponse>> createDepartment(
            @PathVariable Long id, @Valid @RequestBody OrgStructureRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(orgStructureService.createDefaultDepartment(id, request)));
    }

    @DeleteMapping("/departments/{deptId}")
    public ResponseEntity<ApiResponse<Void>> deleteDepartment(@PathVariable Long deptId) {
        orgStructureService.deleteDefaultDepartment(deptId);
        return ResponseEntity.ok(ApiResponse.ok("Department deleted", null));
    }

    // ─── Default user groups for this org type ───────────────────────────

    @GetMapping("/{id}/user-groups")
    public ResponseEntity<ApiResponse<List<OrgStructureResponse>>> getUserGroups(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(orgStructureService.getDefaultUserGroups(id)));
    }

    @PostMapping("/{id}/user-groups")
    public ResponseEntity<ApiResponse<OrgStructureResponse>> createUserGroup(
            @PathVariable Long id, @Valid @RequestBody OrgStructureRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(orgStructureService.createDefaultUserGroup(id, request)));
    }

    @DeleteMapping("/user-groups/{groupId}")
    public ResponseEntity<ApiResponse<Void>> deleteUserGroup(@PathVariable Long groupId) {
        orgStructureService.deleteDefaultUserGroup(groupId);
        return ResponseEntity.ok(ApiResponse.ok("User group deleted", null));
    }
}
