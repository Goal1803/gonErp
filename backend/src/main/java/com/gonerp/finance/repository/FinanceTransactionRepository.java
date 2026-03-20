package com.gonerp.finance.repository;

import com.gonerp.finance.model.FinanceTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FinanceTransactionRepository extends JpaRepository<FinanceTransaction, Long> {

    List<FinanceTransaction> findByMonthlyReportIdAndAccountIdOrderByRowIndexAsc(Long monthlyReportId, Long accountId);

    List<FinanceTransaction> findByMonthlyReportIdOrderByAccountIdAscRowIndexAsc(Long monthlyReportId);

    List<FinanceTransaction> findByTransactionFileId(Long transactionFileId);

    void deleteByTransactionFileId(Long transactionFileId);

    long countByMonthlyReportId(Long monthlyReportId);
}
