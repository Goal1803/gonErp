package com.gonerp.worktime.repository;

import com.gonerp.worktime.model.WorkTimeSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WorkTimeSettingsRepository extends JpaRepository<WorkTimeSettings, Long> {
    Optional<WorkTimeSettings> findByOrganizationId(Long organizationId);
}
