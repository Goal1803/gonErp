package com.gonerp.finance.service;

import com.gonerp.finance.dto.*;
import com.gonerp.finance.model.*;
import com.gonerp.finance.repository.*;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class SteuerberaterService {

    private final FinanceShareLinkRepository shareLinkRepository;
    private final FinanceTransactionRepository transactionRepository;
    private final FinanceInvoiceRepository invoiceRepository;
    private final FinanceAccountRepository accountRepository;
    private final FinanceSteuerberaterCommentRepository commentRepository;

    public FinanceShareLink validateAndAccessLink(String token) {
        FinanceShareLink link = shareLinkRepository.findByToken(token)
                .orElseThrow(() -> new EntityNotFoundException("Invalid share link"));

        if (!link.isActive()) {
            throw new IllegalStateException("Share link is inactive");
        }
        if (link.getExpiresAt() != null && link.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalStateException("Share link has expired");
        }

        link.setLastAccessedAt(LocalDateTime.now());
        link.setAccessCount(link.getAccessCount() + 1);
        shareLinkRepository.save(link);

        return link;
    }

    public MonthlyReportResponse getReport(String token) {
        FinanceShareLink link = validateAndAccessLink(token);
        return MonthlyReportResponse.from(link.getMonthlyReport());
    }

    public List<FinanceAccountResponse> getAccounts(String token) {
        FinanceShareLink link = validateAndAccessLink(token);
        Long orgId = link.getOrganization().getId();
        return accountRepository.findByOrganizationIdAndActiveOrderByDisplayOrderAsc(orgId, true).stream()
                .map(FinanceAccountResponse::from).toList();
    }

    public List<TransactionResponse> getTransactions(String token, Long accountId) {
        FinanceShareLink link = validateAndAccessLink(token);
        Long reportId = link.getMonthlyReport().getId();

        if (accountId != null) {
            return transactionRepository.findByMonthlyReportIdAndAccountIdOrderByRowIndexAsc(reportId, accountId)
                    .stream().map(TransactionResponse::from).toList();
        }
        return transactionRepository.findByMonthlyReportIdOrderByAccountIdAscRowIndexAsc(reportId)
                .stream().map(TransactionResponse::from).toList();
    }

    public List<InvoiceResponse> getInvoices(String token) {
        FinanceShareLink link = validateAndAccessLink(token);
        return invoiceRepository.findByMonthlyReportId(link.getMonthlyReport().getId()).stream()
                .map(InvoiceResponse::from).toList();
    }

    public List<SteuerberaterCommentResponse> getComments(String token) {
        FinanceShareLink link = validateAndAccessLink(token);
        return commentRepository.findByShareLinkIdOrderByCreatedAtAsc(link.getId()).stream()
                .map(SteuerberaterCommentResponse::from).toList();
    }

    public SteuerberaterCommentResponse addComment(String token, SteuerberaterCommentRequest request) {
        FinanceShareLink link = validateAndAccessLink(token);

        FinanceTransaction tx = null;
        if (request.getTransactionId() != null) {
            tx = transactionRepository.findById(request.getTransactionId()).orElse(null);
        }

        FinanceSteuerberaterComment comment = FinanceSteuerberaterComment.builder()
                .shareLink(link)
                .transaction(tx)
                .commentText(request.getCommentText())
                .authorName(request.getAuthorName())
                .build();

        return SteuerberaterCommentResponse.from(commentRepository.save(comment));
    }
}
