package com.gonerp.finance.repository;

import com.gonerp.finance.model.FinanceAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FinanceAccountRepository extends JpaRepository<FinanceAccount, Long> {

    List<FinanceAccount> findByOrganizationIdOrderByDisplayOrderAsc(Long organizationId);

    List<FinanceAccount> findByOrganizationIdAndActiveOrderByDisplayOrderAsc(Long organizationId, boolean active);
}
