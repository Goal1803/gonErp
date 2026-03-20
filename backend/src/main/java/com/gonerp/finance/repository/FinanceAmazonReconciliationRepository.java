package com.gonerp.finance.repository;

import com.gonerp.finance.model.FinanceAmazonReconciliation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FinanceAmazonReconciliationRepository extends JpaRepository<FinanceAmazonReconciliation, Long> {

    List<FinanceAmazonReconciliation> findByMonthlyReportId(Long monthlyReportId);

    Optional<FinanceAmazonReconciliation> findByMonthlyReportIdAndMarketplace(Long monthlyReportId, String marketplace);
}
