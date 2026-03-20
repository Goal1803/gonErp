package com.gonerp.finance.service;

import com.gonerp.finance.dto.FinanceUserRoleRequest;
import com.gonerp.finance.dto.FinanceUserRoleResponse;
import com.gonerp.finance.model.FinanceUserRole;
import com.gonerp.finance.model.enums.FinanceRole;
import com.gonerp.finance.repository.FinanceUserRoleRepository;
import com.gonerp.organization.model.Organization;
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
public class FinanceUserRoleService {

    private final FinanceUserRoleRepository financeUserRoleRepository;
    private final FinanceAccessService financeAccessService;
    private final UserRepository userRepository;

    public List<FinanceUserRoleResponse> findAllForOrg() {
        Organization org = financeAccessService.resolveOrganization();
        return financeUserRoleRepository.findByOrganizationId(org.getId()).stream()
                .map(FinanceUserRoleResponse::from)
                .toList();
    }

    public FinanceUserRoleResponse assign(FinanceUserRoleRequest request) {
        Organization org = financeAccessService.resolveOrganization();
        User targetUser = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + request.getUserId()));

        if (targetUser.getOrganization() == null || !targetUser.getOrganization().getId().equals(org.getId())) {
            throw new IllegalArgumentException("User does not belong to this organization");
        }

        FinanceRole role = FinanceRole.valueOf(request.getFinanceRole());

        FinanceUserRole existing = financeUserRoleRepository
                .findByOrganizationIdAndUserId(org.getId(), targetUser.getId())
                .orElse(null);

        if (existing != null) {
            existing.setFinanceRole(role);
            return FinanceUserRoleResponse.from(financeUserRoleRepository.save(existing));
        }

        FinanceUserRole entity = FinanceUserRole.builder()
                .organization(org)
                .user(targetUser)
                .financeRole(role)
                .build();
        return FinanceUserRoleResponse.from(financeUserRoleRepository.save(entity));
    }

    public void remove(Long userId) {
        Organization org = financeAccessService.resolveOrganization();
        financeUserRoleRepository.deleteByOrganizationIdAndUserId(org.getId(), userId);
    }
}
