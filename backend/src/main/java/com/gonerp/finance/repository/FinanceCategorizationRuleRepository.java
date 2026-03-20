package com.gonerp.finance.repository;

import com.gonerp.finance.model.FinanceCategorizationRule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FinanceCategorizationRuleRepository extends JpaRepository<FinanceCategorizationRule, Long> {

    List<FinanceCategorizationRule> findByOrganizationIdAndActiveTrueOrderByPriorityAsc(Long organizationId);

    List<FinanceCategorizationRule> findByOrganizationIdOrderByPriorityAsc(Long organizationId);
}
