package com.gonerp.worktime.dto;

import com.gonerp.usermanager.model.User;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CalendarUserDTO {
    private Long userId;
    private String userName;
    private String firstName;
    private String lastName;
    private String avatarUrl;

    public static CalendarUserDTO from(User user) {
        return CalendarUserDTO.builder()
                .userId(user.getId())
                .userName(user.getUserName())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .avatarUrl(user.getAvatarUrl())
                .build();
    }
}
