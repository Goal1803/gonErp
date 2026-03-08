package com.gonerp.organization.dto;

import com.gonerp.organization.model.UserUserGroup;
import com.gonerp.organization.model.enums.GroupRole;
import com.gonerp.usermanager.model.User;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class GroupMemberResponse {
    private Long id;
    private String userName;
    private String firstName;
    private String lastName;
    private String avatarUrl;
    private GroupRole groupRole;

    public static GroupMemberResponse from(UserUserGroup uug) {
        User user = uug.getUser();
        return GroupMemberResponse.builder()
                .id(user.getId())
                .userName(user.getUserName())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .avatarUrl(user.getAvatarUrl())
                .groupRole(uug.getGroupRole())
                .build();
    }
}
