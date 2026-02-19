package com.gonerp.usermanager.repository;

import com.gonerp.usermanager.model.UserRole;
import com.gonerp.usermanager.model.enums.RoleName;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRoleRepository extends JpaRepository<UserRole, Long> {
    Optional<UserRole> findByName(RoleName name);
    boolean existsByName(RoleName name);
}
