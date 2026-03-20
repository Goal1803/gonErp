package com.gonerp.finance.repository;

import com.gonerp.finance.model.FinanceInvoice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FinanceInvoiceRepository extends JpaRepository<FinanceInvoice, Long> {

    List<FinanceInvoice> findByMonthlyReportId(Long monthlyReportId);

    List<FinanceInvoice> findByTransactionId(Long transactionId);
}
