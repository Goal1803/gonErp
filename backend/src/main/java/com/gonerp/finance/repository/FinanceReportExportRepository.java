package com.gonerp.finance.repository;

import com.gonerp.finance.model.FinanceReportExport;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FinanceReportExportRepository extends JpaRepository<FinanceReportExport, Long> {

    List<FinanceReportExport> findByMonthlyReportIdOrderByCreatedAtDesc(Long monthlyReportId);

    List<FinanceReportExport> findByOrganizationIdOrderByCreatedAtDesc(Long organizationId);
}
