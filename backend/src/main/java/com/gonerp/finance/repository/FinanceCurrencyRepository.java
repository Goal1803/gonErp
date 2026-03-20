package com.gonerp.finance.repository;

import com.gonerp.finance.model.FinanceCurrency;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FinanceCurrencyRepository extends JpaRepository<FinanceCurrency, Long> {

    List<FinanceCurrency> findByOrganizationIdOrderByCodeAsc(Long organizationId);

    List<FinanceCurrency> findByOrganizationIdAndActiveTrueOrderByCodeAsc(Long organizationId);

    Optional<FinanceCurrency> findByOrganizationIdAndCode(Long organizationId, String code);

    boolean existsByOrganizationIdAndCode(Long organizationId, String code);
}
