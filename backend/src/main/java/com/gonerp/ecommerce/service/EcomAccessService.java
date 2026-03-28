package com.gonerp.ecommerce.service;

import com.gonerp.common.OrgContext;
import com.gonerp.ecommerce.model.EcomStoreMember;
import com.gonerp.ecommerce.model.enums.StoreRole;
import com.gonerp.ecommerce.repository.EcomStoreMemberRepository;
import com.gonerp.organization.model.Organization;
import com.gonerp.organization.repository.OrganizationRepository;
import com.gonerp.usermanager.model.User;
import com.gonerp.usermanager.model.enums.RoleName;
import com.gonerp.usermanager.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EcomAccessService {

    private final EcomStoreMemberRepository ecomStoreMemberRepository;
    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;

    public boolean hasEcommerceAccess() {
        if (OrgContext.isSuperAdmin()) return true;
        User user = OrgContext.getCurrentUser(userRepository);
        if (isOrgAdmin(user)) {
            Organization org = user.getOrganization();
            return org != null && org.isModuleEcommerce();
        }
        Organization org = user.getOrganization();
        if (org == null || !org.isModuleEcommerce()) return false;
        return ecomStoreMemberRepository.existsByStoreOrganizationIdAndUserId(org.getId(), user.getId());
    }

    public void requireEcommerceAccess() {
        if (!hasEcommerceAccess()) {
            throw new SecurityException("No ecommerce access");
        }
    }

    public void requireStoreRole(Long storeId, StoreRole... allowedRoles) {
        if (OrgContext.isSuperAdmin()) return;
        User user = OrgContext.getCurrentUser(userRepository);
        if (isOrgAdmin(user)) return;

        EcomStoreMember member = ecomStoreMemberRepository.findByStoreIdAndUserId(storeId, user.getId())
                .orElseThrow(() -> new SecurityException("No access to store: " + storeId));
        for (StoreRole allowed : allowedRoles) {
            if (member.getStoreRole() == allowed) return;
        }
        throw new SecurityException("Insufficient store role: " + member.getStoreRole());
    }

    /**
     * Resolves the organization for ecommerce operations.
     * Regular users: returns their org.
     * Superadmin: returns the first org with ecommerce module enabled.
     */
    public Organization resolveOrganization() {
        User user = OrgContext.getCurrentUser(userRepository);
        Organization org = user.getOrganization();
        if (org != null) return org;
        // Superadmin fallback
        return organizationRepository.findAll().stream()
                .filter(Organization::isModuleEcommerce)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No organization with ecommerce module enabled"));
    }

    private boolean isOrgAdmin(User user) {
        return user.getRole() != null && user.getRole().getName() == RoleName.ADMIN;
    }
}
