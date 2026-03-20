package com.gonerp.finance.service;

import com.gonerp.common.OrgContext;
import com.gonerp.finance.model.FinanceUserRole;
import com.gonerp.finance.model.enums.FinanceRole;
import com.gonerp.finance.repository.FinanceUserRoleRepository;
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
public class FinanceAccessService {

    private final FinanceUserRoleRepository financeUserRoleRepository;
    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;

    public boolean hasFinanceAccess() {
        if (OrgContext.isSuperAdmin()) return true;
        User user = OrgContext.getCurrentUser(userRepository);
        if (isOrgAdmin(user)) {
            Organization org = user.getOrganization();
            return org != null && org.isModuleFinance();
        }
        Organization org = user.getOrganization();
        if (org == null || !org.isModuleFinance()) return false;
        return financeUserRoleRepository.existsByOrganizationIdAndUserId(org.getId(), user.getId());
    }

    public FinanceRole getCurrentUserFinanceRole() {
        if (OrgContext.isSuperAdmin()) return FinanceRole.FINANCE_CFO;
        User user = OrgContext.getCurrentUser(userRepository);
        if (isOrgAdmin(user)) return FinanceRole.FINANCE_CFO;
        Organization org = user.getOrganization();
        if (org == null) return null;
        return financeUserRoleRepository.findByOrganizationIdAndUserId(org.getId(), user.getId())
                .map(FinanceUserRole::getFinanceRole)
                .orElse(null);
    }

    public void requireFinanceAccess() {
        if (!hasFinanceAccess()) {
            throw new SecurityException("No finance access");
        }
    }

    public void requireFinanceRole(FinanceRole... allowedRoles) {
        FinanceRole role = getCurrentUserFinanceRole();
        if (role == null) {
            throw new SecurityException("No finance access");
        }
        for (FinanceRole allowed : allowedRoles) {
            if (role == allowed) return;
        }
        throw new SecurityException("Insufficient finance role: " + role);
    }

    /**
     * Resolves the organization for finance operations.
     * Regular users: returns their org.
     * Superadmin: returns the first org with finance module enabled.
     */
    public Organization resolveOrganization() {
        User user = OrgContext.getCurrentUser(userRepository);
        Organization org = user.getOrganization();
        if (org != null) return org;
        // Superadmin fallback
        return organizationRepository.findAll().stream()
                .filter(Organization::isModuleFinance)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No organization with finance module enabled"));
    }

    public Organization getOrganizationOr(Organization fallback) {
        User user = OrgContext.getCurrentUser(userRepository);
        Organization org = user.getOrganization();
        return org != null ? org : fallback;
    }

    private boolean isOrgAdmin(User user) {
        return user.getRole() != null && user.getRole().getName() == RoleName.ADMIN;
    }
}
