package com.gonerp.finance.service;

import com.gonerp.finance.dto.FinanceConfigBundle;
import com.gonerp.finance.model.FinanceAccount;
import com.gonerp.finance.model.FinanceCategorizationRule;
import com.gonerp.finance.model.FinanceCurrency;
import com.gonerp.finance.model.enums.AccountType;
import com.gonerp.finance.model.enums.MatchType;
import com.gonerp.finance.repository.FinanceAccountRepository;
import com.gonerp.finance.repository.FinanceCategorizationRuleRepository;
import com.gonerp.finance.repository.FinanceCurrencyRepository;
import com.gonerp.organization.model.Organization;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class FinanceConfigService {

    private final FinanceAccessService financeAccessService;
    private final FinanceCurrencyRepository currencyRepository;
    private final FinanceAccountRepository accountRepository;
    private final FinanceCategorizationRuleRepository ruleRepository;

    public FinanceConfigBundle exportConfig() {
        financeAccessService.requireFinanceAccess();
        Organization org = financeAccessService.resolveOrganization();
        Long orgId = org.getId();

        String username = SecurityContextHolder.getContext().getAuthentication().getName();

        List<FinanceConfigBundle.CurrencyEntry> currencies = currencyRepository
                .findByOrganizationIdOrderByCodeAsc(orgId).stream()
                .map(c -> FinanceConfigBundle.CurrencyEntry.builder()
                        .code(c.getCode())
                        .name(c.getName())
                        .symbol(c.getSymbol())
                        .base(c.isBase())
                        .build())
                .toList();

        List<FinanceConfigBundle.AccountEntry> accounts = accountRepository
                .findByOrganizationIdOrderByDisplayOrderAsc(orgId).stream()
                .map(a -> FinanceConfigBundle.AccountEntry.builder()
                        .name(a.getName())
                        .accountType(a.getAccountType().name())
                        .iban(a.getIban())
                        .currency(a.getCurrency())
                        .marketplace(a.getMarketplace())
                        .csvDelimiter(a.getCsvDelimiter())
                        .csvEncoding(a.getCsvEncoding())
                        .csvSkipRows(a.getCsvSkipRows())
                        .displayOrder(a.getDisplayOrder())
                        .build())
                .toList();

        List<FinanceConfigBundle.RuleEntry> rules = ruleRepository
                .findByOrganizationIdOrderByPriorityAsc(orgId).stream()
                .map(r -> FinanceConfigBundle.RuleEntry.builder()
                        .name(r.getName())
                        .priority(r.getPriority())
                        .active(r.isActive())
                        .accountType(r.getAccountType() != null ? r.getAccountType().name() : null)
                        .fieldName(r.getFieldName())
                        .matchType(r.getMatchType().name())
                        .matchValue(r.getMatchValue())
                        .category(r.getCategory())
                        .subcategory(r.getSubcategory())
                        .noteTemplate(r.getNoteTemplate())
                        .additionalConditions(r.getAdditionalConditions())
                        .build())
                .toList();

        return FinanceConfigBundle.builder()
                .exportedAt(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .exportedBy(username)
                .organizationName(org.getName())
                .currencies(currencies)
                .accounts(accounts)
                .rules(rules)
                .build();
    }

    public String importConfig(FinanceConfigBundle bundle) {
        financeAccessService.requireFinanceAccess();
        Organization org = financeAccessService.resolveOrganization();
        Long orgId = org.getId();

        int currenciesImported = 0;
        int currenciesSkipped = 0;
        int accountsImported = 0;
        int accountsSkipped = 0;
        int rulesImported = 0;
        int rulesSkipped = 0;

        // Import currencies
        if (bundle.getCurrencies() != null) {
            for (FinanceConfigBundle.CurrencyEntry entry : bundle.getCurrencies()) {
                if (currencyRepository.existsByOrganizationIdAndCode(orgId, entry.getCode())) {
                    currenciesSkipped++;
                    continue;
                }
                FinanceCurrency currency = FinanceCurrency.builder()
                        .organization(org)
                        .code(entry.getCode())
                        .name(entry.getName())
                        .symbol(entry.getSymbol())
                        .base(entry.isBase())
                        .active(true)
                        .build();
                currencyRepository.save(currency);
                currenciesImported++;
            }
        }

        // Import accounts - need to check by name
        if (bundle.getAccounts() != null) {
            List<FinanceAccount> existingAccounts = accountRepository.findByOrganizationIdOrderByDisplayOrderAsc(orgId);
            List<String> existingNames = existingAccounts.stream().map(FinanceAccount::getName).toList();

            for (FinanceConfigBundle.AccountEntry entry : bundle.getAccounts()) {
                if (existingNames.contains(entry.getName())) {
                    accountsSkipped++;
                    continue;
                }
                FinanceAccount account = FinanceAccount.builder()
                        .organization(org)
                        .name(entry.getName())
                        .accountType(AccountType.valueOf(entry.getAccountType()))
                        .iban(entry.getIban())
                        .currency(entry.getCurrency())
                        .marketplace(entry.getMarketplace())
                        .csvDelimiter(entry.getCsvDelimiter())
                        .csvEncoding(entry.getCsvEncoding())
                        .csvSkipRows(entry.getCsvSkipRows())
                        .displayOrder(entry.getDisplayOrder())
                        .active(true)
                        .build();
                accountRepository.save(account);
                accountsImported++;
            }
        }

        // Import rules - check by name
        if (bundle.getRules() != null) {
            List<FinanceCategorizationRule> existingRules = ruleRepository.findByOrganizationIdOrderByPriorityAsc(orgId);
            List<String> existingRuleNames = existingRules.stream().map(FinanceCategorizationRule::getName).toList();

            for (FinanceConfigBundle.RuleEntry entry : bundle.getRules()) {
                if (existingRuleNames.contains(entry.getName())) {
                    rulesSkipped++;
                    continue;
                }
                FinanceCategorizationRule rule = FinanceCategorizationRule.builder()
                        .organization(org)
                        .name(entry.getName())
                        .priority(entry.getPriority())
                        .active(entry.isActive())
                        .accountType(entry.getAccountType() != null ? AccountType.valueOf(entry.getAccountType()) : null)
                        .fieldName(entry.getFieldName())
                        .matchType(MatchType.valueOf(entry.getMatchType()))
                        .matchValue(entry.getMatchValue())
                        .category(entry.getCategory())
                        .subcategory(entry.getSubcategory())
                        .noteTemplate(entry.getNoteTemplate())
                        .additionalConditions(entry.getAdditionalConditions())
                        .build();
                ruleRepository.save(rule);
                rulesImported++;
            }
        }

        return String.format("Import complete: %d currencies imported (%d skipped), %d accounts imported (%d skipped), %d rules imported (%d skipped)",
                currenciesImported, currenciesSkipped,
                accountsImported, accountsSkipped,
                rulesImported, rulesSkipped);
    }
}
