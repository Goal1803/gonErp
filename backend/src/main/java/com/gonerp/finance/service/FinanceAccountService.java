package com.gonerp.finance.service;

import com.gonerp.finance.dto.FinanceAccountRequest;
import com.gonerp.finance.dto.FinanceAccountResponse;
import com.gonerp.finance.model.FinanceAccount;
import com.gonerp.finance.model.enums.AccountType;
import com.gonerp.finance.model.enums.FinanceRole;
import com.gonerp.finance.repository.FinanceAccountRepository;
import com.gonerp.organization.model.Organization;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class FinanceAccountService {

    private final FinanceAccountRepository financeAccountRepository;
    private final FinanceAccessService financeAccessService;

    public List<FinanceAccountResponse> findAll() {
        financeAccessService.requireFinanceAccess();
        Organization org = financeAccessService.resolveOrganization();
        return financeAccountRepository.findByOrganizationIdOrderByDisplayOrderAsc(org.getId()).stream()
                .map(FinanceAccountResponse::from)
                .toList();
    }

    public FinanceAccountResponse findById(Long id) {
        financeAccessService.requireFinanceAccess();
        FinanceAccount account = getAccountInOrg(id);
        return FinanceAccountResponse.from(account);
    }

    public FinanceAccountResponse create(FinanceAccountRequest request) {
        financeAccessService.requireFinanceRole(FinanceRole.FINANCE_CFO, FinanceRole.FINANCE_ACCOUNTANT_MANAGER);
        Organization org = financeAccessService.resolveOrganization();

        FinanceAccount account = FinanceAccount.builder()
                .organization(org)
                .name(request.getName())
                .accountType(AccountType.valueOf(request.getAccountType()))
                .iban(request.getIban())
                .currency(request.getCurrency() != null ? request.getCurrency() : "EUR")
                .marketplace(request.getMarketplace())
                .csvDelimiter(request.getCsvDelimiter() != null ? request.getCsvDelimiter() : ";")
                .csvEncoding(request.getCsvEncoding() != null ? request.getCsvEncoding() : "UTF-8")
                .csvSkipRows(request.getCsvSkipRows() != null ? request.getCsvSkipRows() : 0)
                .columnMapping(request.getColumnMapping())
                .active(request.getActive() != null ? request.getActive() : true)
                .displayOrder(request.getDisplayOrder() != null ? request.getDisplayOrder() : 0)
                .build();

        return FinanceAccountResponse.from(financeAccountRepository.save(account));
    }

    public FinanceAccountResponse update(Long id, FinanceAccountRequest request) {
        financeAccessService.requireFinanceRole(FinanceRole.FINANCE_CFO, FinanceRole.FINANCE_ACCOUNTANT_MANAGER);
        FinanceAccount account = getAccountInOrg(id);

        account.setName(request.getName());
        account.setAccountType(AccountType.valueOf(request.getAccountType()));
        account.setIban(request.getIban());
        if (request.getCurrency() != null) account.setCurrency(request.getCurrency());
        account.setMarketplace(request.getMarketplace());
        if (request.getCsvDelimiter() != null) account.setCsvDelimiter(request.getCsvDelimiter());
        if (request.getCsvEncoding() != null) account.setCsvEncoding(request.getCsvEncoding());
        if (request.getCsvSkipRows() != null) account.setCsvSkipRows(request.getCsvSkipRows());
        if (request.getColumnMapping() != null) account.setColumnMapping(request.getColumnMapping());
        if (request.getActive() != null) account.setActive(request.getActive());
        if (request.getDisplayOrder() != null) account.setDisplayOrder(request.getDisplayOrder());

        return FinanceAccountResponse.from(financeAccountRepository.save(account));
    }

    public void delete(Long id) {
        financeAccessService.requireFinanceRole(FinanceRole.FINANCE_CFO, FinanceRole.FINANCE_ACCOUNTANT_MANAGER);
        FinanceAccount account = getAccountInOrg(id);
        financeAccountRepository.delete(account);
    }

    private FinanceAccount getAccountInOrg(Long id) {
        Organization org = financeAccessService.resolveOrganization();
        FinanceAccount account = financeAccountRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Account not found: " + id));
        if (!account.getOrganization().getId().equals(org.getId())) {
            throw new SecurityException("Access denied");
        }
        return account;
    }
}
