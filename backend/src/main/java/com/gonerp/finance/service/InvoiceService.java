package com.gonerp.finance.service;

import com.gonerp.common.FileStorageService;
import com.gonerp.finance.dto.InvoiceResponse;
import com.gonerp.finance.model.FinanceInvoice;
import com.gonerp.finance.model.FinanceMonthlyReport;
import com.gonerp.finance.model.FinanceTransaction;
import com.gonerp.finance.model.enums.InvoiceType;
import com.gonerp.finance.repository.FinanceInvoiceRepository;
import com.gonerp.finance.repository.FinanceMonthlyReportRepository;
import com.gonerp.finance.repository.FinanceTransactionRepository;
import com.gonerp.organization.model.Organization;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class InvoiceService {

    private final FinanceInvoiceRepository invoiceRepository;
    private final FinanceMonthlyReportRepository reportRepository;
    private final FinanceTransactionRepository transactionRepository;
    private final FinanceAccessService financeAccessService;
    private final FileStorageService fileStorageService;

    public List<InvoiceResponse> findByReport(Long reportId) {
        financeAccessService.requireFinanceAccess();
        return invoiceRepository.findByMonthlyReportId(reportId).stream()
                .map(InvoiceResponse::from).toList();
    }

    public List<InvoiceResponse> findByTransaction(Long transactionId) {
        financeAccessService.requireFinanceAccess();
        return invoiceRepository.findByTransactionId(transactionId).stream()
                .map(InvoiceResponse::from).toList();
    }

    public InvoiceResponse upload(Long reportId, Long transactionId, String invoiceType,
                                   String description, MultipartFile file) {
        financeAccessService.requireFinanceAccess();
        Organization org = financeAccessService.resolveOrganization();

        FinanceMonthlyReport report = reportRepository.findById(reportId)
                .orElseThrow(() -> new EntityNotFoundException("Report not found: " + reportId));

        FinanceTransaction tx = null;
        if (transactionId != null) {
            tx = transactionRepository.findById(transactionId).orElse(null);
        }

        String storageUrl;
        try {
            String folder = "finance/" + org.getId() + "/invoices/" + report.getYear() + "/" + report.getMonth();
            storageUrl = fileStorageService.store(file, folder);
        } catch (Exception e) {
            throw new RuntimeException("Failed to upload invoice: " + e.getMessage(), e);
        }

        FinanceInvoice invoice = FinanceInvoice.builder()
                .organization(org)
                .monthlyReport(report)
                .transaction(tx)
                .originalFilename(file.getOriginalFilename())
                .storageUrl(storageUrl)
                .invoiceType(invoiceType != null ? InvoiceType.valueOf(invoiceType) : InvoiceType.INVOICE)
                .description(description)
                .build();

        return InvoiceResponse.from(invoiceRepository.save(invoice));
    }

    public void delete(Long id) {
        financeAccessService.requireFinanceAccess();
        Organization org = financeAccessService.resolveOrganization();
        FinanceInvoice invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Invoice not found: " + id));
        if (!invoice.getOrganization().getId().equals(org.getId())) {
            throw new SecurityException("Access denied");
        }
        invoiceRepository.delete(invoice);
    }
}
