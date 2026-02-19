package com.gonerp.config;

import com.gonerp.usermanager.model.User;
import com.gonerp.usermanager.model.UserRole;
import com.gonerp.usermanager.model.enums.RoleName;
import com.gonerp.usermanager.model.enums.UserStatus;
import com.gonerp.usermanager.repository.UserRepository;
import com.gonerp.usermanager.repository.UserRoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private final UserRoleRepository userRoleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(ApplicationArguments args) {
        initRoles();
        initAdminUser();
    }

    private void initRoles() {
        if (!userRoleRepository.existsByName(RoleName.ADMIN)) {
            userRoleRepository.save(UserRole.builder()
                    .name(RoleName.ADMIN)
                    .description("System Administrator with full access")
                    .build());
            log.info("Created ADMIN role");
        }
        if (!userRoleRepository.existsByName(RoleName.USER)) {
            userRoleRepository.save(UserRole.builder()
                    .name(RoleName.USER)
                    .description("Standard user with limited access")
                    .build());
            log.info("Created USER role");
        }
    }

    private void initAdminUser() {
        if (!userRepository.existsByUserName("admin")) {
            UserRole adminRole = userRoleRepository.findByName(RoleName.ADMIN).orElseThrow();
            User admin = User.builder()
                    .userName("admin")
                    .firstName("System")
                    .lastName("Admin")
                    .status(UserStatus.ACTIVE)
                    .password(passwordEncoder.encode("admin123"))
                    .role(adminRole)
                    .build();
            userRepository.save(admin);
            log.info("Created default admin user (username: admin, password: admin123)");
        }
    }
}
