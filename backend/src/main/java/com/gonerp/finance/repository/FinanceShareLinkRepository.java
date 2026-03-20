package com.gonerp.finance.repository;

import com.gonerp.finance.model.FinanceShareLink;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FinanceShareLinkRepository extends JpaRepository<FinanceShareLink, Long> {

    Optional<FinanceShareLink> findByToken(String token);

    List<FinanceShareLink> findByMonthlyReportId(Long monthlyReportId);

    List<FinanceShareLink> findByOrganizationId(Long organizationId);
}
