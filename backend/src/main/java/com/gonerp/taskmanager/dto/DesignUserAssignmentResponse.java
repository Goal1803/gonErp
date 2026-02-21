package com.gonerp.taskmanager.dto;

import com.gonerp.organization.dto.OrgStructureResponse;
import com.gonerp.taskmanager.model.UserDesignStaffRole;
import com.gonerp.taskmanager.model.UserDesignUserGroup;
import com.gonerp.usermanager.model.User;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class DesignUserAssignmentResponse {
    private Long userId;
    private String userName;
    private String firstName;
    private String lastName;
    private List<OrgStructureResponse> designStaffRoles;
    private List<OrgStructureResponse> designUserGroups;

    public static DesignUserAssignmentResponse from(User user,
                                                     List<UserDesignStaffRole> staffRoles,
                                                     List<UserDesignUserGroup> userGroups) {
        return DesignUserAssignmentResponse.builder()
                .userId(user.getId())
                .userName(user.getUserName())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .designStaffRoles(staffRoles.stream()
                        .map(usr -> OrgStructureResponse.builder()
                                .id(usr.getDesignStaffRole().getId())
                                .name(usr.getDesignStaffRole().getName())
                                .description(usr.getDesignStaffRole().getDescription())
                                .build())
                        .toList())
                .designUserGroups(userGroups.stream()
                        .map(uug -> OrgStructureResponse.builder()
                                .id(uug.getDesignUserGroup().getId())
                                .name(uug.getDesignUserGroup().getName())
                                .description(uug.getDesignUserGroup().getDescription())
                                .build())
                        .toList())
                .build();
    }
}
