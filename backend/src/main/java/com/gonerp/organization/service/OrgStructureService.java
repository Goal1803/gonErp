package com.gonerp.organization.service;

import com.gonerp.common.OrgContext;
import com.gonerp.organization.dto.OrgStructureRequest;
import com.gonerp.organization.dto.OrgStructureResponse;
import com.gonerp.organization.model.*;
import com.gonerp.organization.repository.*;
import com.gonerp.usermanager.model.User;
import com.gonerp.usermanager.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class OrgStructureService {

    private final StaffRoleRepository staffRoleRepository;
    private final DepartmentRepository departmentRepository;
    private final UserGroupRepository userGroupRepository;
    private final UserStaffRoleRepository userStaffRoleRepository;
    private final UserDepartmentRepository userDepartmentRepository;
    private final UserUserGroupRepository userUserGroupRepository;
    private final OrganizationTypeRepository orgTypeRepository;
    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;

    // ─── Default CRUD (SUPER_ADMIN — linked to org type) ─────────────────

    public List<OrgStructureResponse> getDefaultStaffRoles(Long orgTypeId) {
        return staffRoleRepository.findByOrgTypeId(orgTypeId).stream()
                .map(r -> toResponse(r, true)).toList();
    }

    public OrgStructureResponse createDefaultStaffRole(Long orgTypeId, OrgStructureRequest request) {
        OrganizationType orgType = orgTypeRepository.findById(orgTypeId)
                .orElseThrow(() -> new EntityNotFoundException("Org type not found: " + orgTypeId));
        StaffRole role = StaffRole.builder()
                .name(request.getName()).description(request.getDescription()).orgType(orgType).build();
        return toResponse(staffRoleRepository.save(role), true);
    }

    public void deleteDefaultStaffRole(Long id) {
        staffRoleRepository.deleteById(id);
    }

    public List<OrgStructureResponse> getDefaultDepartments(Long orgTypeId) {
        return departmentRepository.findByOrgTypeId(orgTypeId).stream()
                .map(d -> toResponse(d, true)).toList();
    }

    public OrgStructureResponse createDefaultDepartment(Long orgTypeId, OrgStructureRequest request) {
        OrganizationType orgType = orgTypeRepository.findById(orgTypeId)
                .orElseThrow(() -> new EntityNotFoundException("Org type not found: " + orgTypeId));
        Department dept = Department.builder()
                .name(request.getName()).description(request.getDescription()).orgType(orgType).build();
        return toResponse(departmentRepository.save(dept), true);
    }

    public void deleteDefaultDepartment(Long id) {
        departmentRepository.deleteById(id);
    }

    public List<OrgStructureResponse> getDefaultUserGroups(Long orgTypeId) {
        return userGroupRepository.findByOrgTypeId(orgTypeId).stream()
                .map(g -> toResponse(g, true)).toList();
    }

    public OrgStructureResponse createDefaultUserGroup(Long orgTypeId, OrgStructureRequest request) {
        OrganizationType orgType = orgTypeRepository.findById(orgTypeId)
                .orElseThrow(() -> new EntityNotFoundException("Org type not found: " + orgTypeId));
        UserGroup group = UserGroup.builder()
                .name(request.getName()).description(request.getDescription()).orgType(orgType).build();
        return toResponse(userGroupRepository.save(group), true);
    }

    public void deleteDefaultUserGroup(Long id) {
        userGroupRepository.deleteById(id);
    }

    // ─── Combined listing (for org admins) ───────────────────────────────

    public List<OrgStructureResponse> getStaffRolesForCurrentOrg() {
        Organization org = OrgContext.requireOrganization(userRepository);
        return getCombinedStaffRoles(org);
    }

    public List<OrgStructureResponse> getDepartmentsForCurrentOrg() {
        Organization org = OrgContext.requireOrganization(userRepository);
        return getCombinedDepartments(org);
    }

    public List<OrgStructureResponse> getUserGroupsForCurrentOrg() {
        Organization org = OrgContext.requireOrganization(userRepository);
        return getCombinedUserGroups(org);
    }

    private List<OrgStructureResponse> getCombinedStaffRoles(Organization org) {
        List<OrgStructureResponse> result = new ArrayList<>();
        if (org.getOrgType() != null) {
            staffRoleRepository.findByOrgTypeId(org.getOrgType().getId())
                    .forEach(r -> result.add(toResponse(r, true)));
        }
        staffRoleRepository.findByOrganizationId(org.getId())
                .forEach(r -> result.add(toResponse(r, false)));
        return result;
    }

    private List<OrgStructureResponse> getCombinedDepartments(Organization org) {
        List<OrgStructureResponse> result = new ArrayList<>();
        if (org.getOrgType() != null) {
            departmentRepository.findByOrgTypeId(org.getOrgType().getId())
                    .forEach(d -> result.add(toResponse(d, true)));
        }
        departmentRepository.findByOrganizationId(org.getId())
                .forEach(d -> result.add(toResponse(d, false)));
        return result;
    }

    private List<OrgStructureResponse> getCombinedUserGroups(Organization org) {
        List<OrgStructureResponse> result = new ArrayList<>();
        if (org.getOrgType() != null) {
            userGroupRepository.findByOrgTypeId(org.getOrgType().getId())
                    .forEach(g -> result.add(toResponse(g, true)));
        }
        userGroupRepository.findByOrganizationId(org.getId())
                .forEach(g -> result.add(toResponse(g, false)));
        return result;
    }

    // ─── Org-extra CRUD (ADMIN — linked to their org) ────────────────────

    public OrgStructureResponse createOrgStaffRole(OrgStructureRequest request) {
        Organization org = OrgContext.requireOrganization(userRepository);
        StaffRole role = StaffRole.builder()
                .name(request.getName()).description(request.getDescription()).organization(org).build();
        return toResponse(staffRoleRepository.save(role), false);
    }

    public OrgStructureResponse updateOrgStaffRole(Long id, OrgStructureRequest request) {
        StaffRole role = staffRoleRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Staff role not found: " + id));
        if (role.getOrgType() != null) {
            throw new AccessDeniedException("Cannot edit a default staff role");
        }
        role.setName(request.getName());
        role.setDescription(request.getDescription());
        return toResponse(staffRoleRepository.save(role), false);
    }

    public void deleteOrgStaffRole(Long id) {
        StaffRole role = staffRoleRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Staff role not found: " + id));
        if (role.getOrgType() != null) {
            throw new AccessDeniedException("Cannot delete a default staff role");
        }
        staffRoleRepository.delete(role);
    }

    public OrgStructureResponse createOrgDepartment(OrgStructureRequest request) {
        Organization org = OrgContext.requireOrganization(userRepository);
        Department dept = Department.builder()
                .name(request.getName()).description(request.getDescription()).organization(org).build();
        return toResponse(departmentRepository.save(dept), false);
    }

    public OrgStructureResponse updateOrgDepartment(Long id, OrgStructureRequest request) {
        Department dept = departmentRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Department not found: " + id));
        if (dept.getOrgType() != null) {
            throw new AccessDeniedException("Cannot edit a default department");
        }
        dept.setName(request.getName());
        dept.setDescription(request.getDescription());
        return toResponse(departmentRepository.save(dept), false);
    }

    public void deleteOrgDepartment(Long id) {
        Department dept = departmentRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Department not found: " + id));
        if (dept.getOrgType() != null) {
            throw new AccessDeniedException("Cannot delete a default department");
        }
        departmentRepository.delete(dept);
    }

    public OrgStructureResponse createOrgUserGroup(OrgStructureRequest request) {
        Organization org = OrgContext.requireOrganization(userRepository);
        UserGroup group = UserGroup.builder()
                .name(request.getName()).description(request.getDescription()).organization(org).build();
        return toResponse(userGroupRepository.save(group), false);
    }

    public OrgStructureResponse updateOrgUserGroup(Long id, OrgStructureRequest request) {
        UserGroup group = userGroupRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User group not found: " + id));
        if (group.getOrgType() != null) {
            throw new AccessDeniedException("Cannot edit a default user group");
        }
        group.setName(request.getName());
        group.setDescription(request.getDescription());
        return toResponse(userGroupRepository.save(group), false);
    }

    public void deleteOrgUserGroup(Long id) {
        UserGroup group = userGroupRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User group not found: " + id));
        if (group.getOrgType() != null) {
            throw new AccessDeniedException("Cannot delete a default user group");
        }
        userGroupRepository.delete(group);
    }

    // ─── User assignments ────────────────────────────────────────────────

    public void assignStaffRole(Long userId, Long staffRoleId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + userId));
        StaffRole role = staffRoleRepository.findById(staffRoleId)
                .orElseThrow(() -> new EntityNotFoundException("Staff role not found: " + staffRoleId));
        if (!userStaffRoleRepository.existsByUserIdAndStaffRoleId(userId, staffRoleId)) {
            userStaffRoleRepository.save(UserStaffRole.builder().user(user).staffRole(role).build());
        }
    }

    public void removeStaffRole(Long userId, Long staffRoleId) {
        userStaffRoleRepository.deleteByUserIdAndStaffRoleId(userId, staffRoleId);
    }

    public void assignDepartment(Long userId, Long departmentId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + userId));
        Department dept = departmentRepository.findById(departmentId)
                .orElseThrow(() -> new EntityNotFoundException("Department not found: " + departmentId));
        if (!userDepartmentRepository.existsByUserIdAndDepartmentId(userId, departmentId)) {
            userDepartmentRepository.save(UserDepartment.builder().user(user).department(dept).build());
        }
    }

    public void removeDepartment(Long userId, Long departmentId) {
        userDepartmentRepository.deleteByUserIdAndDepartmentId(userId, departmentId);
    }

    public void assignUserGroup(Long userId, Long userGroupId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + userId));
        UserGroup group = userGroupRepository.findById(userGroupId)
                .orElseThrow(() -> new EntityNotFoundException("User group not found: " + userGroupId));
        if (!userUserGroupRepository.existsByUserIdAndUserGroupId(userId, userGroupId)) {
            userUserGroupRepository.save(UserUserGroup.builder().user(user).userGroup(group).build());
        }
    }

    public void removeUserGroup(Long userId, Long userGroupId) {
        userUserGroupRepository.deleteByUserIdAndUserGroupId(userId, userGroupId);
    }

    // ─── Helpers ─────────────────────────────────────────────────────────

    private OrgStructureResponse toResponse(StaffRole r, boolean isDefault) {
        return OrgStructureResponse.builder()
                .id(r.getId()).name(r.getName()).description(r.getDescription()).isDefault(isDefault).build();
    }

    private OrgStructureResponse toResponse(Department d, boolean isDefault) {
        return OrgStructureResponse.builder()
                .id(d.getId()).name(d.getName()).description(d.getDescription()).isDefault(isDefault).build();
    }

    private OrgStructureResponse toResponse(UserGroup g, boolean isDefault) {
        return OrgStructureResponse.builder()
                .id(g.getId()).name(g.getName()).description(g.getDescription()).isDefault(isDefault).build();
    }
}
