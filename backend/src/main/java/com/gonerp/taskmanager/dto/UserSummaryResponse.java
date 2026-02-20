package com.gonerp.taskmanager.dto;

import com.gonerp.usermanager.model.User;
import lombok.Data;

@Data
public class UserSummaryResponse {
    private Long id;
    private String userName;
    private String firstName;
    private String lastName;
    private String avatarUrl;

    public static UserSummaryResponse from(User user) {
        UserSummaryResponse r = new UserSummaryResponse();
        r.id = user.getId();
        r.userName = user.getUserName();
        r.firstName = user.getFirstName();
        r.lastName = user.getLastName();
        r.avatarUrl = user.getAvatarUrl();
        return r;
    }
}
