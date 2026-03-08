package com.gonerp.worktime.repository;

import com.gonerp.worktime.model.UserWorkTimeConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserWorkTimeConfigRepository extends JpaRepository<UserWorkTimeConfig, Long> {
    Optional<UserWorkTimeConfig> findByUserId(Long userId);

    List<UserWorkTimeConfig> findByUserOrganizationId(Long organizationId);
}
