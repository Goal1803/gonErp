package com.gonerp.taskmanager.service;

import com.gonerp.common.OrgContext;
import com.gonerp.organization.dto.OrgStructureRequest;
import com.gonerp.organization.dto.OrgStructureResponse;
import com.gonerp.organization.model.Organization;
import com.gonerp.taskmanager.dto.DesignUserAssignmentResponse;
import com.gonerp.taskmanager.model.DesignStaffRole;
import com.gonerp.taskmanager.model.DesignUserGroup;
import com.gonerp.taskmanager.model.UserDesignStaffRole;
import com.gonerp.taskmanager.model.UserDesignUserGroup;
import com.gonerp.taskmanager.repository.DesignStaffRoleRepository;
import com.gonerp.taskmanager.repository.DesignUserGroupRepository;
import com.gonerp.taskmanager.repository.UserDesignStaffRoleRepository;
import com.gonerp.taskmanager.repository.UserDesignUserGroupRepository;
import com.gonerp.usermanager.model.User;
import com.gonerp.usermanager.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class DesignConfigService {

    private final DesignStaffRoleRepository staffRoleRepository;
    private final DesignUserGroupRepository userGroupRepository;
    private final UserDesignStaffRoleRepository userStaffRoleRepository;
    private final UserDesignUserGroupRepository userUserGroupRepository;
    private final UserRepository userRepository;

    // ─── Staff Roles CRUD (SUPER_ADMIN) ──────────────────────────────────

    public List<OrgStructureResponse> getAllStaffRoles() {
        return staffRoleRepository.findAll().stream()
                .map(this::toResponse).toList();
    }

    public OrgStructureResponse createStaffRole(OrgStructureRequest request) {
        if (staffRoleRepository.existsByName(request.getName())) {
            throw new IllegalArgumentException("Design staff role '" + request.getName() + "' already exists");
        }
        DesignStaffRole role = DesignStaffRole.builder()
                .name(request.getName()).description(request.getDescription()).build();
        return toResponse(staffRoleRepository.save(role));
    }

    public OrgStructureResponse updateStaffRole(Long id, OrgStructureRequest request) {
        DesignStaffRole role = staffRoleRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Design staff role not found: " + id));
        if (staffRoleRepository.existsByNameAndIdNot(request.getName(), id)) {
            throw new IllegalArgumentException("Design staff role '" + request.getName() + "' already exists");
        }
        role.setName(request.getName());
        role.setDescription(request.getDescription());
        return toResponse(staffRoleRepository.save(role));
    }

    public void deleteStaffRole(Long id) {
        staffRoleRepository.deleteById(id);
    }

    // ─── User Groups CRUD (SUPER_ADMIN) ──────────────────────────────────

    public List<OrgStructureResponse> getAllUserGroups() {
        return userGroupRepository.findAll().stream()
                .map(this::toResponse).toList();
    }

    public OrgStructureResponse createUserGroup(OrgStructureRequest request) {
        if (userGroupRepository.existsByName(request.getName())) {
            throw new IllegalArgumentException("Design user group '" + request.getName() + "' already exists");
        }
        DesignUserGroup group = DesignUserGroup.builder()
                .name(request.getName()).description(request.getDescription()).build();
        return toResponse(userGroupRepository.save(group));
    }

    public OrgStructureResponse updateUserGroup(Long id, OrgStructureRequest request) {
        DesignUserGroup group = userGroupRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Design user group not found: " + id));
        if (userGroupRepository.existsByNameAndIdNot(request.getName(), id)) {
            throw new IllegalArgumentException("Design user group '" + request.getName() + "' already exists");
        }
        group.setName(request.getName());
        group.setDescription(request.getDescription());
        return toResponse(userGroupRepository.save(group));
    }

    public void deleteUserGroup(Long id) {
        userGroupRepository.deleteById(id);
    }

    // ─── User Assignments (ADMIN) ────────────────────────────────────────

    public List<DesignUserAssignmentResponse> getOrgUsersWithAssignments() {
        Organization org = OrgContext.requireOrganization(userRepository);
        List<User> users = userRepository.findByOrganizationId(org.getId());

        return users.stream().map(user -> {
            List<UserDesignStaffRole> staffRoles = userStaffRoleRepository.findByUserId(user.getId());
            List<UserDesignUserGroup> userGroups = userUserGroupRepository.findByUserId(user.getId());
            return DesignUserAssignmentResponse.from(user, staffRoles, userGroups);
        }).toList();
    }

    public void assignStaffRole(Long userId, Long roleId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + userId));
        DesignStaffRole role = staffRoleRepository.findById(roleId)
                .orElseThrow(() -> new EntityNotFoundException("Design staff role not found: " + roleId));
        if (!userStaffRoleRepository.existsByUserIdAndDesignStaffRoleId(userId, roleId)) {
            userStaffRoleRepository.save(UserDesignStaffRole.builder().user(user).designStaffRole(role).build());
        }
    }

    public void removeStaffRole(Long userId, Long roleId) {
        userStaffRoleRepository.deleteByUserIdAndDesignStaffRoleId(userId, roleId);
    }

    public void assignUserGroup(Long userId, Long groupId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + userId));
        DesignUserGroup group = userGroupRepository.findById(groupId)
                .orElseThrow(() -> new EntityNotFoundException("Design user group not found: " + groupId));
        if (!userUserGroupRepository.existsByUserIdAndDesignUserGroupId(userId, groupId)) {
            userUserGroupRepository.save(UserDesignUserGroup.builder().user(user).designUserGroup(group).build());
        }
    }

    public void removeUserGroup(Long userId, Long groupId) {
        userUserGroupRepository.deleteByUserIdAndDesignUserGroupId(userId, groupId);
    }

    // ─── Helpers ─────────────────────────────────────────────────────────

    private OrgStructureResponse toResponse(DesignStaffRole r) {
        return OrgStructureResponse.builder()
                .id(r.getId()).name(r.getName()).description(r.getDescription()).build();
    }

    private OrgStructureResponse toResponse(DesignUserGroup g) {
        return OrgStructureResponse.builder()
                .id(g.getId()).name(g.getName()).description(g.getDescription()).build();
    }
}
