package com.gonerp.config;

import com.gonerp.organization.model.*;
import com.gonerp.organization.repository.*;
import com.gonerp.taskmanager.model.Board;
import com.gonerp.taskmanager.repository.BoardRepository;
import com.gonerp.usermanager.model.User;
import com.gonerp.usermanager.model.UserRole;
import com.gonerp.usermanager.model.enums.RoleName;
import com.gonerp.usermanager.model.enums.UserStatus;
import com.gonerp.usermanager.repository.UserRepository;
import com.gonerp.usermanager.repository.UserRoleRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private final UserRoleRepository userRoleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final OrganizationTypeRepository orgTypeRepository;
    private final OrganizationRepository organizationRepository;
    private final BoardRepository boardRepository;
    private final StaffRoleRepository staffRoleRepository;
    private final DepartmentRepository departmentRepository;
    private final UserGroupRepository userGroupRepository;
    private final EntityManager entityManager;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        dropEnumConstraints();
        makeDesignCardIdNullable();
        initRoles();
        initDefaultOrgType();
        initDefaultOrganization();
        initSuperAdmin();
        migrateExistingData();
        initAdminUser();
    }

    private void dropEnumConstraints() {
        try {
            entityManager.createNativeQuery(
                    "ALTER TABLE user_roles DROP CONSTRAINT IF EXISTS user_roles_name_check"
            ).executeUpdate();
            log.info("Dropped user_roles_name_check constraint (if existed)");
        } catch (Exception e) {
            log.warn("Could not drop user_roles_name_check constraint: {}", e.getMessage());
        }
    }

    private void makeDesignCardIdNullable() {
        try {
            entityManager.createNativeQuery(
                    "ALTER TABLE tm_design_details ALTER COLUMN card_id DROP NOT NULL"
            ).executeUpdate();
            log.info("Made tm_design_details.card_id nullable (if needed)");
        } catch (Exception e) {
            log.warn("Could not alter tm_design_details.card_id: {}", e.getMessage());
        }
    }

    private void initRoles() {
        if (!userRoleRepository.existsByName(RoleName.SUPER_ADMIN)) {
            userRoleRepository.save(UserRole.builder()
                    .name(RoleName.SUPER_ADMIN)
                    .description("Platform Super Administrator")
                    .build());
            log.info("Created SUPER_ADMIN role");
        }
        if (!userRoleRepository.existsByName(RoleName.ADMIN)) {
            userRoleRepository.save(UserRole.builder()
                    .name(RoleName.ADMIN)
                    .description("Organization Administrator with full access")
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

    private void initDefaultOrgType() {
        if (!orgTypeRepository.existsByName("Default Type")) {
            OrganizationType defaultType = orgTypeRepository.save(OrganizationType.builder()
                    .name("Default Type")
                    .description("Default organization type")
                    .build());
            log.info("Created 'Default Type' organization type");

            // Default staff roles
            List.of("CEO", "Manager", "Designer", "Developer", "Salesman").forEach(name ->
                    staffRoleRepository.save(StaffRole.builder().name(name).orgType(defaultType).build()));
            log.info("Created default staff roles for 'Default Type'");

            // Default departments
            List.of("Management", "Design", "Development", "Sales", "Marketing").forEach(name ->
                    departmentRepository.save(Department.builder().name(name).orgType(defaultType).build()));
            log.info("Created default departments for 'Default Type'");

            // Default user groups
            userGroupRepository.save(UserGroup.builder().name("All Staff").orgType(defaultType).build());
            log.info("Created default user groups for 'Default Type'");
        }
    }

    private void initDefaultOrganization() {
        if (!organizationRepository.existsBySlug("default")) {
            OrganizationType defaultType = orgTypeRepository.findByName("Default Type").orElseThrow();
            organizationRepository.save(Organization.builder()
                    .name("Default Organization")
                    .slug("default")
                    .orgType(defaultType)
                    .build());
            log.info("Created 'Default Organization'");
        }
    }

    private void initSuperAdmin() {
        if (!userRepository.existsByUserName("superadmin")) {
            UserRole superAdminRole = userRoleRepository.findByName(RoleName.SUPER_ADMIN).orElseThrow();
            User superAdmin = User.builder()
                    .userName("superadmin")
                    .firstName("Super")
                    .lastName("Admin")
                    .status(UserStatus.ACTIVE)
                    .password(passwordEncoder.encode("superadmin123"))
                    .role(superAdminRole)
                    .build();
            userRepository.save(superAdmin);
            log.info("Created superadmin user (username: superadmin, password: superadmin123)");
        }
    }

    private void migrateExistingData() {
        Organization defaultOrg = organizationRepository.findBySlug("default").orElse(null);
        if (defaultOrg == null) return;

        // Assign all users without org (except SUPER_ADMIN) to default org
        UserRole superAdminRole = userRoleRepository.findByName(RoleName.SUPER_ADMIN).orElse(null);
        List<User> usersWithoutOrg = userRepository.findAll().stream()
                .filter(u -> u.getOrganization() == null)
                .filter(u -> superAdminRole == null || !u.getRole().getId().equals(superAdminRole.getId()))
                .toList();
        for (User user : usersWithoutOrg) {
            user.setOrganization(defaultOrg);
            userRepository.save(user);
        }
        if (!usersWithoutOrg.isEmpty()) {
            log.info("Migrated {} users to 'Default Organization'", usersWithoutOrg.size());
        }

        // Assign all boards without org to default org
        List<Board> boardsWithoutOrg = boardRepository.findAll().stream()
                .filter(b -> b.getOrganization() == null)
                .toList();
        for (Board board : boardsWithoutOrg) {
            board.setOrganization(defaultOrg);
            boardRepository.save(board);
        }
        if (!boardsWithoutOrg.isEmpty()) {
            log.info("Migrated {} boards to 'Default Organization'", boardsWithoutOrg.size());
        }
    }

    private void initAdminUser() {
        if (!userRepository.existsByUserName("admin")) {
            UserRole adminRole = userRoleRepository.findByName(RoleName.ADMIN).orElseThrow();
            Organization defaultOrg = organizationRepository.findBySlug("default").orElse(null);
            User admin = User.builder()
                    .userName("admin")
                    .firstName("System")
                    .lastName("Admin")
                    .status(UserStatus.ACTIVE)
                    .password(passwordEncoder.encode("admin123"))
                    .role(adminRole)
                    .organization(defaultOrg)
                    .build();
            userRepository.save(admin);
            log.info("Created default admin user (username: admin, password: admin123)");
        }
    }
}
