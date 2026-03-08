package com.gonerp.organization.service;

import com.gonerp.common.OrgContext;
import com.gonerp.organization.dto.OrgStructureRequest;
import com.gonerp.organization.dto.OrgStructureResponse;
import com.gonerp.organization.model.*;
import com.gonerp.organization.repository.*;
import com.gonerp.usermanager.model.User;
import com.gonerp.usermanager.repository.UserRepository;
import com.gonerp.organization.dto.GroupMemberResponse;
import com.gonerp.organization.model.enums.GroupRole;
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
        if (staffRoleRepository.existsByNameAndOrgTypeId(request.getName(), orgTypeId)) {
            throw new IllegalArgumentException("Staff role '" + request.getName() + "' already exists for this org type");
        }
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
        if (departmentRepository.existsByNameAndOrgTypeId(request.getName(), orgTypeId)) {
            throw new IllegalArgumentException("Department '" + request.getName() + "' already exists for this org type");
        }
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
        if (userGroupRepository.existsByNameAndOrgTypeId(request.getName(), orgTypeId)) {
            throw new IllegalArgumentException("User group '" + request.getName() + "' already exists for this org type");
        }
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
        if (staffRoleRepository.existsByNameAndOrganizationId(request.getName(), org.getId())) {
            throw new IllegalArgumentException("Staff role '" + request.getName() + "' already exists in this organization");
        }
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
        if (staffRoleRepository.existsByNameAndOrganizationIdAndIdNot(request.getName(), role.getOrganization().getId(), id)) {
            throw new IllegalArgumentException("Staff role '" + request.getName() + "' already exists in this organization");
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
        if (departmentRepository.existsByNameAndOrganizationId(request.getName(), org.getId())) {
            throw new IllegalArgumentException("Department '" + request.getName() + "' already exists in this organization");
        }
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
        if (departmentRepository.existsByNameAndOrganizationIdAndIdNot(request.getName(), dept.getOrganization().getId(), id)) {
            throw new IllegalArgumentException("Department '" + request.getName() + "' already exists in this organization");
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
        if (userGroupRepository.existsByNameAndOrganizationId(request.getName(), org.getId())) {
            throw new IllegalArgumentException("User group '" + request.getName() + "' already exists in this organization");
        }
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
        if (userGroupRepository.existsByNameAndOrganizationIdAndIdNot(request.getName(), group.getOrganization().getId(), id)) {
            throw new IllegalArgumentException("User group '" + request.getName() + "' already exists in this organization");
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
            // First member becomes OWNER
            boolean hasOwner = userUserGroupRepository.findByUserGroupIdAndGroupRole(userGroupId, GroupRole.OWNER).isPresent();
            GroupRole role = hasOwner ? GroupRole.MEMBER : GroupRole.OWNER;
            userUserGroupRepository.save(UserUserGroup.builder()
                    .user(user).userGroup(group).groupRole(role).build());
        }
    }

    public void removeUserGroup(Long userId, Long userGroupId) {
        UserUserGroup uug = userUserGroupRepository.findByUserIdAndUserGroupId(userId, userGroupId)
                .orElseThrow(() -> new EntityNotFoundException("User is not a member of this group"));
        if (uug.getGroupRole() == GroupRole.OWNER) {
            throw new IllegalArgumentException("Cannot remove the group owner. Transfer ownership first.");
        }
        userUserGroupRepository.delete(uug);
    }

    // ─── Group members ─────────────────────────────────────────────────

    public List<GroupMemberResponse> getGroupMembers(Long groupId) {
        userGroupRepository.findById(groupId)
                .orElseThrow(() -> new EntityNotFoundException("User group not found: " + groupId));
        return userUserGroupRepository.findByUserGroupId(groupId).stream()
                .map(GroupMemberResponse::from)
                .toList();
    }

    public void changeGroupRole(Long userId, Long userGroupId, GroupRole newRole) {
        UserUserGroup uug = userUserGroupRepository.findByUserIdAndUserGroupId(userId, userGroupId)
                .orElseThrow(() -> new EntityNotFoundException("User is not a member of this group"));
        if (uug.getGroupRole() == GroupRole.OWNER) {
            throw new IllegalArgumentException("Cannot change the owner's role. Transfer ownership first.");
        }
        if (newRole == GroupRole.OWNER) {
            throw new IllegalArgumentException("Use the transfer ownership endpoint to assign a new owner.");
        }
        uug.setGroupRole(newRole);
        userUserGroupRepository.save(uug);
    }

    public void transferOwnership(Long newOwnerUserId, Long userGroupId) {
        // Demote current owner
        UserUserGroup currentOwner = userUserGroupRepository.findByUserGroupIdAndGroupRole(userGroupId, GroupRole.OWNER)
                .orElseThrow(() -> new EntityNotFoundException("No owner found for this group"));
        currentOwner.setGroupRole(GroupRole.ADMIN);
        userUserGroupRepository.save(currentOwner);

        // Promote new owner
        UserUserGroup newOwner = userUserGroupRepository.findByUserIdAndUserGroupId(newOwnerUserId, userGroupId)
                .orElseThrow(() -> new EntityNotFoundException("User is not a member of this group"));
        newOwner.setGroupRole(GroupRole.OWNER);
        userUserGroupRepository.save(newOwner);
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
