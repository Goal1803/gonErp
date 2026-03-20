package com.gonerp.finance.service;

import com.gonerp.finance.dto.*;
import com.gonerp.finance.model.FinanceCurrency;
import com.gonerp.finance.model.FinanceCurrencyRate;
import com.gonerp.finance.model.enums.FinanceRole;
import com.gonerp.finance.repository.FinanceCurrencyRateRepository;
import com.gonerp.finance.repository.FinanceCurrencyRepository;
import com.gonerp.organization.model.Organization;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class CurrencyService {

    private final FinanceCurrencyRepository currencyRepository;
    private final FinanceCurrencyRateRepository rateRepository;
    private final FinanceAccessService financeAccessService;

    // --- Currency CRUD ---

    public List<CurrencyResponse> findAllCurrencies() {
        financeAccessService.requireFinanceAccess();
        Organization org = financeAccessService.resolveOrganization();
        return currencyRepository.findByOrganizationIdOrderByCodeAsc(org.getId()).stream()
                .map(CurrencyResponse::from).toList();
    }

    public CurrencyResponse createCurrency(CurrencyRequest request) {
        financeAccessService.requireFinanceRole(FinanceRole.FINANCE_CFO, FinanceRole.FINANCE_ACCOUNTANT_MANAGER);
        Organization org = financeAccessService.resolveOrganization();

        if (currencyRepository.existsByOrganizationIdAndCode(org.getId(), request.getCode().toUpperCase())) {
            throw new IllegalArgumentException("Currency already exists: " + request.getCode());
        }

        FinanceCurrency currency = FinanceCurrency.builder()
                .organization(org)
                .code(request.getCode().toUpperCase())
                .name(request.getName())
                .symbol(request.getSymbol())
                .base(request.getBase() != null ? request.getBase() : false)
                .active(request.getActive() != null ? request.getActive() : true)
                .build();

        return CurrencyResponse.from(currencyRepository.save(currency));
    }

    public CurrencyResponse updateCurrency(Long id, CurrencyRequest request) {
        financeAccessService.requireFinanceRole(FinanceRole.FINANCE_CFO, FinanceRole.FINANCE_ACCOUNTANT_MANAGER);
        Organization org = financeAccessService.resolveOrganization();
        FinanceCurrency currency = currencyRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Currency not found"));
        if (!currency.getOrganization().getId().equals(org.getId())) throw new SecurityException("Access denied");

        currency.setName(request.getName());
        if (request.getSymbol() != null) currency.setSymbol(request.getSymbol());
        if (request.getBase() != null) currency.setBase(request.getBase());
        if (request.getActive() != null) currency.setActive(request.getActive());

        return CurrencyResponse.from(currencyRepository.save(currency));
    }

    public void deleteCurrency(Long id) {
        financeAccessService.requireFinanceRole(FinanceRole.FINANCE_CFO, FinanceRole.FINANCE_ACCOUNTANT_MANAGER);
        Organization org = financeAccessService.resolveOrganization();
        FinanceCurrency currency = currencyRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Currency not found"));
        if (!currency.getOrganization().getId().equals(org.getId())) throw new SecurityException("Access denied");
        currencyRepository.delete(currency);
    }

    // --- Currency Rates ---

    public List<CurrencyRateResponse> findAllRates() {
        financeAccessService.requireFinanceAccess();
        Organization org = financeAccessService.resolveOrganization();
        return rateRepository.findByOrganizationIdOrderByEffectiveDateDescFromCurrencyAsc(org.getId()).stream()
                .map(CurrencyRateResponse::from).toList();
    }

    public CurrencyRateResponse createRate(CurrencyRateRequest request) {
        financeAccessService.requireFinanceRole(FinanceRole.FINANCE_CFO, FinanceRole.FINANCE_ACCOUNTANT_MANAGER);
        Organization org = financeAccessService.resolveOrganization();

        FinanceCurrencyRate rate = FinanceCurrencyRate.builder()
                .organization(org)
                .fromCurrency(request.getFromCurrency().toUpperCase())
                .toCurrency(request.getToCurrency().toUpperCase())
                .rate(request.getRate())
                .effectiveDate(request.getEffectiveDate())
                .build();

        return CurrencyRateResponse.from(rateRepository.save(rate));
    }

    public void deleteRate(Long id) {
        financeAccessService.requireFinanceRole(FinanceRole.FINANCE_CFO, FinanceRole.FINANCE_ACCOUNTANT_MANAGER);
        Organization org = financeAccessService.resolveOrganization();
        FinanceCurrencyRate rate = rateRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Rate not found"));
        if (!rate.getOrganization().getId().equals(org.getId())) throw new SecurityException("Access denied");
        rateRepository.delete(rate);
    }

    public BigDecimal convert(BigDecimal amount, String from, String to, LocalDate date) {
        if (from.equalsIgnoreCase(to)) return amount;
        Organization org = financeAccessService.resolveOrganization();
        FinanceCurrencyRate rate = rateRepository.findRateForDate(org.getId(), from.toUpperCase(), to.toUpperCase(), date)
                .orElseThrow(() -> new IllegalStateException("No exchange rate found for " + from + " -> " + to + " on " + date));
        return amount.multiply(rate.getRate());
    }
}
