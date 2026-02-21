package com.gonerp.taskmanager.service;

import com.gonerp.common.OrgContext;
import com.gonerp.organization.dto.OrgStructureRequest;
import com.gonerp.organization.dto.OrgStructureResponse;
import com.gonerp.organization.model.Organization;
import com.gonerp.taskmanager.dto.TaskUserAssignmentResponse;
import com.gonerp.taskmanager.model.TaskStaffRole;
import com.gonerp.taskmanager.model.TaskUserGroup;
import com.gonerp.taskmanager.model.UserTaskStaffRole;
import com.gonerp.taskmanager.model.UserTaskUserGroup;
import com.gonerp.taskmanager.repository.TaskStaffRoleRepository;
import com.gonerp.taskmanager.repository.TaskUserGroupRepository;
import com.gonerp.taskmanager.repository.UserTaskStaffRoleRepository;
import com.gonerp.taskmanager.repository.UserTaskUserGroupRepository;
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
public class TaskConfigService {

    private final TaskStaffRoleRepository staffRoleRepository;
    private final TaskUserGroupRepository userGroupRepository;
    private final UserTaskStaffRoleRepository userStaffRoleRepository;
    private final UserTaskUserGroupRepository userUserGroupRepository;
    private final UserRepository userRepository;

    // ─── Staff Roles CRUD (SUPER_ADMIN) ──────────────────────────────────

    public List<OrgStructureResponse> getAllStaffRoles() {
        return staffRoleRepository.findAll().stream()
                .map(this::toResponse).toList();
    }

    public OrgStructureResponse createStaffRole(OrgStructureRequest request) {
        if (staffRoleRepository.existsByName(request.getName())) {
            throw new IllegalArgumentException("Task staff role '" + request.getName() + "' already exists");
        }
        TaskStaffRole role = TaskStaffRole.builder()
                .name(request.getName()).description(request.getDescription()).build();
        return toResponse(staffRoleRepository.save(role));
    }

    public OrgStructureResponse updateStaffRole(Long id, OrgStructureRequest request) {
        TaskStaffRole role = staffRoleRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Task staff role not found: " + id));
        if (staffRoleRepository.existsByNameAndIdNot(request.getName(), id)) {
            throw new IllegalArgumentException("Task staff role '" + request.getName() + "' already exists");
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
            throw new IllegalArgumentException("Task user group '" + request.getName() + "' already exists");
        }
        TaskUserGroup group = TaskUserGroup.builder()
                .name(request.getName()).description(request.getDescription()).build();
        return toResponse(userGroupRepository.save(group));
    }

    public OrgStructureResponse updateUserGroup(Long id, OrgStructureRequest request) {
        TaskUserGroup group = userGroupRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Task user group not found: " + id));
        if (userGroupRepository.existsByNameAndIdNot(request.getName(), id)) {
            throw new IllegalArgumentException("Task user group '" + request.getName() + "' already exists");
        }
        group.setName(request.getName());
        group.setDescription(request.getDescription());
        return toResponse(userGroupRepository.save(group));
    }

    public void deleteUserGroup(Long id) {
        userGroupRepository.deleteById(id);
    }

    // ─── User Assignments (ADMIN) ────────────────────────────────────────

    public List<TaskUserAssignmentResponse> getOrgUsersWithAssignments() {
        Organization org = OrgContext.requireOrganization(userRepository);
        List<User> users = userRepository.findByOrganizationId(org.getId());

        return users.stream().map(user -> {
            List<UserTaskStaffRole> staffRoles = userStaffRoleRepository.findByUserId(user.getId());
            List<UserTaskUserGroup> userGroups = userUserGroupRepository.findByUserId(user.getId());
            return TaskUserAssignmentResponse.from(user, staffRoles, userGroups);
        }).toList();
    }

    public void assignStaffRole(Long userId, Long roleId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + userId));
        TaskStaffRole role = staffRoleRepository.findById(roleId)
                .orElseThrow(() -> new EntityNotFoundException("Task staff role not found: " + roleId));
        if (!userStaffRoleRepository.existsByUserIdAndTaskStaffRoleId(userId, roleId)) {
            userStaffRoleRepository.save(UserTaskStaffRole.builder().user(user).taskStaffRole(role).build());
        }
    }

    public void removeStaffRole(Long userId, Long roleId) {
        userStaffRoleRepository.deleteByUserIdAndTaskStaffRoleId(userId, roleId);
    }

    public void assignUserGroup(Long userId, Long groupId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + userId));
        TaskUserGroup group = userGroupRepository.findById(groupId)
                .orElseThrow(() -> new EntityNotFoundException("Task user group not found: " + groupId));
        if (!userUserGroupRepository.existsByUserIdAndTaskUserGroupId(userId, groupId)) {
            userUserGroupRepository.save(UserTaskUserGroup.builder().user(user).taskUserGroup(group).build());
        }
    }

    public void removeUserGroup(Long userId, Long groupId) {
        userUserGroupRepository.deleteByUserIdAndTaskUserGroupId(userId, groupId);
    }

    // ─── Helpers ─────────────────────────────────────────────────────────

    private OrgStructureResponse toResponse(TaskStaffRole r) {
        return OrgStructureResponse.builder()
                .id(r.getId()).name(r.getName()).description(r.getDescription()).build();
    }

    private OrgStructureResponse toResponse(TaskUserGroup g) {
        return OrgStructureResponse.builder()
                .id(g.getId()).name(g.getName()).description(g.getDescription()).build();
    }
}
