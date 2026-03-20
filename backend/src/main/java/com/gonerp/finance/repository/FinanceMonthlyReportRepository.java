package com.gonerp.finance.repository;

import com.gonerp.finance.model.FinanceMonthlyReport;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FinanceMonthlyReportRepository extends JpaRepository<FinanceMonthlyReport, Long> {

    List<FinanceMonthlyReport> findByOrganizationIdOrderByYearDescMonthDesc(Long organizationId);

    Optional<FinanceMonthlyReport> findByOrganizationIdAndYearAndMonth(Long organizationId, int year, int month);

    boolean existsByOrganizationIdAndYearAndMonth(Long organizationId, int year, int month);
}
