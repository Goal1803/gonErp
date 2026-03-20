package com.gonerp.finance.repository;

import com.gonerp.finance.model.FinanceTransactionFile;
import com.gonerp.finance.model.enums.UploadFileType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FinanceTransactionFileRepository extends JpaRepository<FinanceTransactionFile, Long> {

    List<FinanceTransactionFile> findByMonthlyReportId(Long monthlyReportId);

    List<FinanceTransactionFile> findByMonthlyReportIdAndAccountId(Long monthlyReportId, Long accountId);

    boolean existsByMonthlyReportIdAndAccountIdAndFileType(Long monthlyReportId, Long accountId, UploadFileType fileType);
}
