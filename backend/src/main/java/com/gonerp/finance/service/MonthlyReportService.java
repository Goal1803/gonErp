package com.gonerp.finance.service;

import com.gonerp.finance.dto.MonthlyReportRequest;
import com.gonerp.finance.dto.MonthlyReportResponse;
import com.gonerp.finance.model.FinanceMonthlyReport;
import com.gonerp.finance.model.enums.FinanceRole;
import com.gonerp.finance.model.enums.ReportStatus;
import com.gonerp.finance.repository.FinanceMonthlyReportRepository;
import com.gonerp.organization.model.Organization;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class MonthlyReportService {

    private final FinanceMonthlyReportRepository reportRepository;
    private final FinanceAccessService financeAccessService;

    public List<MonthlyReportResponse> findAll() {
        financeAccessService.requireFinanceAccess();
        Organization org = financeAccessService.resolveOrganization();
        return reportRepository.findByOrganizationIdOrderByYearDescMonthDesc(org.getId()).stream()
                .map(MonthlyReportResponse::from)
                .toList();
    }

    public MonthlyReportResponse findById(Long id) {
        financeAccessService.requireFinanceAccess();
        FinanceMonthlyReport report = getReportInOrg(id);
        return MonthlyReportResponse.from(report);
    }

    public MonthlyReportResponse create(MonthlyReportRequest request) {
        financeAccessService.requireFinanceRole(FinanceRole.FINANCE_CFO, FinanceRole.FINANCE_ACCOUNTANT_MANAGER);
        Organization org = financeAccessService.resolveOrganization();

        if (reportRepository.existsByOrganizationIdAndYearAndMonth(org.getId(), request.getYear(), request.getMonth())) {
            throw new IllegalArgumentException("Monthly report already exists for " + request.getYear() + "/" + request.getMonth());
        }

        FinanceMonthlyReport report = FinanceMonthlyReport.builder()
                .organization(org)
                .year(request.getYear())
                .month(request.getMonth())
                .status(ReportStatus.DRAFT)
                .notes(request.getNotes())
                .build();

        return MonthlyReportResponse.from(reportRepository.save(report));
    }

    public MonthlyReportResponse update(Long id, MonthlyReportRequest request) {
        financeAccessService.requireFinanceRole(FinanceRole.FINANCE_CFO, FinanceRole.FINANCE_ACCOUNTANT_MANAGER);
        FinanceMonthlyReport report = getReportInOrg(id);

        if (request.getNotes() != null) report.setNotes(request.getNotes());
        if (request.getStatus() != null) report.setStatus(ReportStatus.valueOf(request.getStatus()));

        return MonthlyReportResponse.from(reportRepository.save(report));
    }

    public void delete(Long id) {
        financeAccessService.requireFinanceRole(FinanceRole.FINANCE_CFO);
        FinanceMonthlyReport report = getReportInOrg(id);
        reportRepository.delete(report);
    }

    private FinanceMonthlyReport getReportInOrg(Long id) {
        Organization org = financeAccessService.resolveOrganization();
        FinanceMonthlyReport report = reportRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Monthly report not found: " + id));
        if (!report.getOrganization().getId().equals(org.getId())) {
            throw new SecurityException("Access denied");
        }
        return report;
    }
}
