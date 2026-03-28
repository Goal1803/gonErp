package com.gonerp.ecommerce.repository;

import com.gonerp.ecommerce.model.EcomStoreMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EcomStoreMemberRepository extends JpaRepository<EcomStoreMember, Long> {

    List<EcomStoreMember> findByStoreId(Long storeId);

    Optional<EcomStoreMember> findByStoreIdAndUserId(Long storeId, Long userId);

    boolean existsByStoreIdAndUserId(Long storeId, Long userId);

    boolean existsByStoreOrganizationIdAndUserId(Long organizationId, Long userId);

    void deleteByStoreIdAndUserId(Long storeId, Long userId);
}
