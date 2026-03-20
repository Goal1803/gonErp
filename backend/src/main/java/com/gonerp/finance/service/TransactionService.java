package com.gonerp.finance.service;

import com.gonerp.finance.dto.TransactionResponse;
import com.gonerp.finance.dto.TransactionUpdateRequest;
import com.gonerp.finance.model.FinanceTransaction;
import com.gonerp.finance.repository.FinanceTransactionRepository;
import com.gonerp.organization.model.Organization;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class TransactionService {

    private final FinanceTransactionRepository transactionRepository;
    private final FinanceAccessService financeAccessService;

    public List<TransactionResponse> findByReportAndAccount(Long reportId, Long accountId) {
        financeAccessService.requireFinanceAccess();
        return transactionRepository.findByMonthlyReportIdAndAccountIdOrderByRowIndexAsc(reportId, accountId)
                .stream().map(TransactionResponse::from).toList();
    }

    public List<TransactionResponse> findByReport(Long reportId) {
        financeAccessService.requireFinanceAccess();
        return transactionRepository.findByMonthlyReportIdOrderByAccountIdAscRowIndexAsc(reportId)
                .stream().map(TransactionResponse::from).toList();
    }

    public TransactionResponse update(Long id, TransactionUpdateRequest request) {
        financeAccessService.requireFinanceAccess();
        FinanceTransaction tx = getTransactionInOrg(id);

        if (request.getCategory() != null) tx.setCategory(request.getCategory());
        if (request.getSubcategory() != null) tx.setSubcategory(request.getSubcategory());
        if (request.getNote() != null) tx.setNote(request.getNote());
        tx.setManuallyReviewed(true);

        return TransactionResponse.from(transactionRepository.save(tx));
    }

    public TransactionResponse toggleCompleted(Long id) {
        financeAccessService.requireFinanceAccess();
        FinanceTransaction tx = getTransactionInOrg(id);
        tx.setCompleted(!tx.isCompleted());
        return TransactionResponse.from(transactionRepository.save(tx));
    }

    public TransactionResponse resetOne(Long id) {
        financeAccessService.requireFinanceAccess();
        FinanceTransaction tx = getTransactionInOrg(id);
        tx.setCategory(null);
        tx.setSubcategory(null);
        tx.setNote(null);
        tx.setAutoCategorized(false);
        tx.setManuallyReviewed(false);
        tx.setCompleted(false);
        return TransactionResponse.from(transactionRepository.save(tx));
    }

    public int resetAllForReport(Long reportId) {
        financeAccessService.requireFinanceAccess();
        List<FinanceTransaction> transactions = transactionRepository
                .findByMonthlyReportIdOrderByAccountIdAscRowIndexAsc(reportId);
        int count = 0;
        for (FinanceTransaction tx : transactions) {
            tx.setCategory(null);
            tx.setSubcategory(null);
            tx.setNote(null);
            tx.setAutoCategorized(false);
            tx.setManuallyReviewed(false);
            tx.setCompleted(false);
            count++;
        }
        transactionRepository.saveAll(transactions);
        return count;
    }

    private FinanceTransaction getTransactionInOrg(Long id) {
        Organization org = financeAccessService.resolveOrganization();
        FinanceTransaction tx = transactionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Transaction not found: " + id));
        if (!tx.getOrganization().getId().equals(org.getId())) {
            throw new SecurityException("Access denied");
        }
        return tx;
    }
}
