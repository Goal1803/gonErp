package com.gonerp.ecommerce.service;

import com.gonerp.ecommerce.dto.EcomStoreMemberRequest;
import com.gonerp.ecommerce.dto.EcomStoreMemberResponse;
import com.gonerp.ecommerce.model.EcomStore;
import com.gonerp.ecommerce.model.EcomStoreMember;
import com.gonerp.ecommerce.model.enums.StoreRole;
import com.gonerp.ecommerce.repository.EcomStoreMemberRepository;
import com.gonerp.ecommerce.repository.EcomStoreRepository;
import com.gonerp.usermanager.model.User;
import com.gonerp.usermanager.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class EcomStoreMemberService {

    private final EcomStoreMemberRepository ecomStoreMemberRepository;
    private final EcomStoreRepository ecomStoreRepository;
    private final EcomAccessService ecomAccessService;
    private final UserRepository userRepository;

    public List<EcomStoreMemberResponse> findByStore(Long storeId) {
        ecomAccessService.requireEcommerceAccess();
        return ecomStoreMemberRepository.findByStoreId(storeId).stream()
                .map(EcomStoreMemberResponse::from)
                .toList();
    }

    public EcomStoreMemberResponse assign(Long storeId, EcomStoreMemberRequest request) {
        ecomAccessService.requireEcommerceAccess();
        EcomStore store = ecomStoreRepository.findById(storeId)
                .orElseThrow(() -> new EntityNotFoundException("Store not found: " + storeId));

        User targetUser = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + request.getUserId()));

        if (targetUser.getOrganization() == null ||
                !targetUser.getOrganization().getId().equals(store.getOrganization().getId())) {
            throw new IllegalArgumentException("User does not belong to this organization");
        }

        StoreRole role = StoreRole.valueOf(request.getStoreRole());

        EcomStoreMember existing = ecomStoreMemberRepository
                .findByStoreIdAndUserId(storeId, targetUser.getId())
                .orElse(null);

        if (existing != null) {
            existing.setStoreRole(role);
            return EcomStoreMemberResponse.from(ecomStoreMemberRepository.save(existing));
        }

        EcomStoreMember entity = EcomStoreMember.builder()
                .store(store)
                .user(targetUser)
                .storeRole(role)
                .build();
        return EcomStoreMemberResponse.from(ecomStoreMemberRepository.save(entity));
    }

    public void remove(Long storeId, Long userId) {
        ecomAccessService.requireEcommerceAccess();
        ecomStoreMemberRepository.deleteByStoreIdAndUserId(storeId, userId);
    }
}
