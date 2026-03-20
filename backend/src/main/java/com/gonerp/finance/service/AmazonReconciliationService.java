package com.gonerp.finance.service;

import com.gonerp.finance.dto.AmazonReconciliationResponse;
import com.gonerp.finance.model.FinanceAmazonReconciliation;
import com.gonerp.finance.model.FinanceMonthlyReport;
import com.gonerp.finance.model.FinanceTransaction;
import com.gonerp.finance.model.enums.AccountType;
import com.gonerp.finance.repository.FinanceAmazonReconciliationRepository;
import com.gonerp.finance.repository.FinanceMonthlyReportRepository;
import com.gonerp.finance.repository.FinanceTransactionRepository;
import com.gonerp.organization.model.Organization;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional
public class AmazonReconciliationService {

    private final FinanceAmazonReconciliationRepository reconciliationRepository;
    private final FinanceTransactionRepository transactionRepository;
    private final FinanceMonthlyReportRepository reportRepository;
    private final FinanceAccessService financeAccessService;

    public List<AmazonReconciliationResponse> findByReport(Long reportId) {
        financeAccessService.requireFinanceAccess();
        return reconciliationRepository.findByMonthlyReportId(reportId).stream()
                .map(AmazonReconciliationResponse::from).toList();
    }

    public AmazonReconciliationResponse generate(Long reportId, String marketplace) {
        financeAccessService.requireFinanceAccess();
        Organization org = financeAccessService.resolveOrganization();

        FinanceMonthlyReport report = reportRepository.findById(reportId)
                .orElseThrow(() -> new EntityNotFoundException("Report not found: " + reportId));

        List<FinanceTransaction> transactions = transactionRepository
                .findByMonthlyReportIdOrderByAccountIdAscRowIndexAsc(reportId);

        // Filter Amazon transactions for this marketplace
        List<FinanceTransaction> amazonTxs = transactions.stream()
                .filter(tx -> tx.getAccount().getAccountType() == AccountType.AMAZON_SELLER)
                .filter(tx -> marketplace == null || marketplace.equals(tx.getAccount().getMarketplace()))
                .toList();

        BigDecimal totalSales = BigDecimal.ZERO;
        BigDecimal totalFees = BigDecimal.ZERO;
        BigDecimal totalPayouts = BigDecimal.ZERO;

        Map<String, Object> reconciliationData = new LinkedHashMap<>();
        Map<String, BigDecimal> byType = new LinkedHashMap<>();

        for (FinanceTransaction tx : amazonTxs) {
            String type = tx.getRawData() != null ? tx.getRawData().getOrDefault("type", "Other") : "Other";
            BigDecimal amount = tx.getAmount() != null ? tx.getAmount() : BigDecimal.ZERO;

            byType.merge(type, amount, BigDecimal::add);

            String typeLower = type.toLowerCase();
            if (typeLower.contains("order") || typeLower.contains("sale")) {
                totalSales = totalSales.add(amount);
            } else if (typeLower.contains("fee") || typeLower.contains("service")) {
                totalFees = totalFees.add(amount);
            } else if (typeLower.contains("transfer") || typeLower.contains("payout")) {
                totalPayouts = totalPayouts.add(amount);
            }
        }

        reconciliationData.put("transactionCount", amazonTxs.size());
        reconciliationData.put("byType", byType);

        BigDecimal discrepancy = totalSales.add(totalFees).subtract(totalPayouts.abs());

        // Upsert reconciliation
        FinanceAmazonReconciliation recon = reconciliationRepository
                .findByMonthlyReportIdAndMarketplace(reportId, marketplace != null ? marketplace : "ALL")
                .orElse(FinanceAmazonReconciliation.builder()
                        .organization(org)
                        .monthlyReport(report)
                        .marketplace(marketplace != null ? marketplace : "ALL")
                        .build());

        recon.setReconciliationData(reconciliationData);
        recon.setTotalSales(totalSales);
        recon.setTotalFees(totalFees);
        recon.setTotalPayouts(totalPayouts);
        recon.setDiscrepancy(discrepancy);

        return AmazonReconciliationResponse.from(reconciliationRepository.save(recon));
    }
}
