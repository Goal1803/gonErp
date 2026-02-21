package com.gonerp.taskmanager.dto;

import com.gonerp.organization.dto.OrgStructureResponse;
import com.gonerp.taskmanager.model.UserTaskStaffRole;
import com.gonerp.taskmanager.model.UserTaskUserGroup;
import com.gonerp.usermanager.model.User;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class TaskUserAssignmentResponse {
    private Long userId;
    private String userName;
    private String firstName;
    private String lastName;
    private List<OrgStructureResponse> taskStaffRoles;
    private List<OrgStructureResponse> taskUserGroups;

    public static TaskUserAssignmentResponse from(User user,
                                                    List<UserTaskStaffRole> staffRoles,
                                                    List<UserTaskUserGroup> userGroups) {
        return TaskUserAssignmentResponse.builder()
                .userId(user.getId())
                .userName(user.getUserName())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .taskStaffRoles(staffRoles.stream()
                        .map(usr -> OrgStructureResponse.builder()
                                .id(usr.getTaskStaffRole().getId())
                                .name(usr.getTaskStaffRole().getName())
                                .description(usr.getTaskStaffRole().getDescription())
                                .build())
                        .toList())
                .taskUserGroups(userGroups.stream()
                        .map(uug -> OrgStructureResponse.builder()
                                .id(uug.getTaskUserGroup().getId())
                                .name(uug.getTaskUserGroup().getName())
                                .description(uug.getTaskUserGroup().getDescription())
                                .build())
                        .toList())
                .build();
    }
}
