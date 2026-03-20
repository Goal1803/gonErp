package com.gonerp.finance.repository;

import com.gonerp.finance.model.FinanceUserRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FinanceUserRoleRepository extends JpaRepository<FinanceUserRole, Long> {

    List<FinanceUserRole> findByOrganizationId(Long organizationId);

    Optional<FinanceUserRole> findByOrganizationIdAndUserId(Long organizationId, Long userId);

    boolean existsByOrganizationIdAndUserId(Long organizationId, Long userId);

    void deleteByOrganizationIdAndUserId(Long organizationId, Long userId);
}
