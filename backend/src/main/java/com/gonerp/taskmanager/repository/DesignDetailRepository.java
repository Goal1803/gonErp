package com.gonerp.taskmanager.repository;

import com.gonerp.taskmanager.model.DesignDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DesignDetailRepository extends JpaRepository<DesignDetail, Long>, JpaSpecificationExecutor<DesignDetail> {
    Optional<DesignDetail> findByCardId(Long cardId);
}
