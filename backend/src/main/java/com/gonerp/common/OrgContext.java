package com.gonerp.common;

import com.gonerp.organization.model.Organization;
import com.gonerp.usermanager.model.User;
import com.gonerp.usermanager.model.enums.RoleName;
import com.gonerp.usermanager.repository.UserRepository;
import org.springframework.security.core.context.SecurityContextHolder;

public class OrgContext {

    private OrgContext() {}

    public static User getCurrentUser(UserRepository userRepository) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUserName(username)
                .orElseThrow(() -> new RuntimeException("Current user not found"));
    }

    public static Organization getCurrentOrganization(UserRepository userRepository) {
        User user = getCurrentUser(userRepository);
        return user.getOrganization();
    }

    public static Organization requireOrganization(UserRepository userRepository) {
        Organization org = getCurrentOrganization(userRepository);
        if (org == null) {
            throw new IllegalStateException("Current user has no organization");
        }
        return org;
    }

    public static boolean isSuperAdmin(UserRepository userRepository) {
        User user = getCurrentUser(userRepository);
        return user.getRole().getName() == RoleName.SUPER_ADMIN;
    }

    public static boolean isSuperAdmin() {
        return SecurityContextHolder.getContext().getAuthentication().getAuthorities()
                .stream().anyMatch(a -> a.getAuthority().equals("ROLE_SUPER_ADMIN"));
    }
}
