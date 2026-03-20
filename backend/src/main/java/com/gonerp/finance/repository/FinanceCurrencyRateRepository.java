package com.gonerp.finance.repository;

import com.gonerp.finance.model.FinanceCurrencyRate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface FinanceCurrencyRateRepository extends JpaRepository<FinanceCurrencyRate, Long> {

    List<FinanceCurrencyRate> findByOrganizationIdOrderByEffectiveDateDescFromCurrencyAsc(Long organizationId);

    List<FinanceCurrencyRate> findByOrganizationIdAndFromCurrencyAndToCurrencyOrderByEffectiveDateDesc(
            Long organizationId, String fromCurrency, String toCurrency);

    @Query("SELECT r FROM FinanceCurrencyRate r WHERE r.organization.id = :orgId " +
           "AND r.fromCurrency = :from AND r.toCurrency = :to " +
           "AND r.effectiveDate <= :date ORDER BY r.effectiveDate DESC LIMIT 1")
    Optional<FinanceCurrencyRate> findRateForDate(@Param("orgId") Long orgId,
            @Param("from") String from, @Param("to") String to, @Param("date") LocalDate date);
}
