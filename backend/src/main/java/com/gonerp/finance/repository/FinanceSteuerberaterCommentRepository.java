package com.gonerp.finance.repository;

import com.gonerp.finance.model.FinanceSteuerberaterComment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FinanceSteuerberaterCommentRepository extends JpaRepository<FinanceSteuerberaterComment, Long> {

    List<FinanceSteuerberaterComment> findByShareLinkIdOrderByCreatedAtAsc(Long shareLinkId);

    List<FinanceSteuerberaterComment> findByTransactionIdOrderByCreatedAtAsc(Long transactionId);
}
