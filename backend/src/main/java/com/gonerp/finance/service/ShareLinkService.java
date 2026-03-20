package com.gonerp.finance.service;

import com.gonerp.finance.dto.ShareLinkRequest;
import com.gonerp.finance.dto.ShareLinkResponse;
import com.gonerp.finance.model.FinanceMonthlyReport;
import com.gonerp.finance.model.FinanceShareLink;
import com.gonerp.finance.model.enums.FinanceRole;
import com.gonerp.finance.repository.FinanceMonthlyReportRepository;
import com.gonerp.finance.repository.FinanceShareLinkRepository;
import com.gonerp.organization.model.Organization;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ShareLinkService {

    private final FinanceShareLinkRepository shareLinkRepository;
    private final FinanceMonthlyReportRepository reportRepository;
    private final FinanceAccessService financeAccessService;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    public List<ShareLinkResponse> findByOrg() {
        financeAccessService.requireFinanceRole(FinanceRole.FINANCE_CFO);
        Organization org = financeAccessService.resolveOrganization();
        return shareLinkRepository.findByOrganizationId(org.getId()).stream()
                .map(ShareLinkResponse::from).toList();
    }

    public List<ShareLinkResponse> findByReport(Long reportId) {
        financeAccessService.requireFinanceRole(FinanceRole.FINANCE_CFO);
        return shareLinkRepository.findByMonthlyReportId(reportId).stream()
                .map(ShareLinkResponse::from).toList();
    }

    public ShareLinkResponse create(ShareLinkRequest request) {
        financeAccessService.requireFinanceRole(FinanceRole.FINANCE_CFO);
        Organization org = financeAccessService.resolveOrganization();

        FinanceMonthlyReport report = reportRepository.findById(request.getMonthlyReportId())
                .orElseThrow(() -> new EntityNotFoundException("Report not found"));

        String token = generateToken();

        FinanceShareLink link = FinanceShareLink.builder()
                .organization(org)
                .monthlyReport(report)
                .token(token)
                .recipientName(request.getRecipientName())
                .recipientEmail(request.getRecipientEmail())
                .expiresAt(request.getExpiresAt())
                .build();

        return ShareLinkResponse.from(shareLinkRepository.save(link));
    }

    public ShareLinkResponse toggleActive(Long id) {
        financeAccessService.requireFinanceRole(FinanceRole.FINANCE_CFO);
        Organization org = financeAccessService.resolveOrganization();
        FinanceShareLink link = shareLinkRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Share link not found: " + id));
        if (!link.getOrganization().getId().equals(org.getId())) throw new SecurityException("Access denied");

        link.setActive(!link.isActive());
        return ShareLinkResponse.from(shareLinkRepository.save(link));
    }

    public void delete(Long id) {
        financeAccessService.requireFinanceRole(FinanceRole.FINANCE_CFO);
        Organization org = financeAccessService.resolveOrganization();
        FinanceShareLink link = shareLinkRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Share link not found: " + id));
        if (!link.getOrganization().getId().equals(org.getId())) throw new SecurityException("Access denied");
        shareLinkRepository.delete(link);
    }

    private String generateToken() {
        byte[] bytes = new byte[96];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
