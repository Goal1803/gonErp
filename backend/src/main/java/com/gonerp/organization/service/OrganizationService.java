package com.gonerp.organization.service;

import com.gonerp.organization.dto.OrganizationRequest;
import com.gonerp.organization.dto.OrganizationResponse;
import com.gonerp.organization.model.Organization;
import com.gonerp.organization.model.OrganizationType;
import com.gonerp.organization.repository.OrganizationRepository;
import com.gonerp.organization.repository.OrganizationTypeRepository;
import com.gonerp.usermanager.model.User;
import com.gonerp.usermanager.model.UserRole;
import com.gonerp.usermanager.model.enums.RoleName;
import com.gonerp.usermanager.model.enums.UserStatus;
import com.gonerp.usermanager.repository.UserRepository;
import com.gonerp.usermanager.repository.UserRoleRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class OrganizationService {

    private final OrganizationRepository organizationRepository;
    private final OrganizationTypeRepository orgTypeRepository;
    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final PasswordEncoder passwordEncoder;

    public List<OrganizationResponse> findAll() {
        return organizationRepository.findAll().stream()
                .map(org -> OrganizationResponse.from(org, userRepository.countByOrganizationId(org.getId())))
                .toList();
    }

    public OrganizationResponse findById(Long id) {
        Organization org = organizationRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Organization not found: " + id));
        return OrganizationResponse.from(org, userRepository.countByOrganizationId(org.getId()));
    }

    public OrganizationResponse create(OrganizationRequest request) {
        if (organizationRepository.existsByName(request.getName())) {
            throw new IllegalArgumentException("Organization name already exists: " + request.getName());
        }
        if (organizationRepository.existsBySlug(request.getSlug())) {
            throw new IllegalArgumentException("Organization slug already exists: " + request.getSlug());
        }

        OrganizationType orgType = orgTypeRepository.findById(request.getOrgTypeId())
                .orElseThrow(() -> new EntityNotFoundException("Organization type not found: " + request.getOrgTypeId()));

        Organization org = Organization.builder()
                .name(request.getName())
                .slug(request.getSlug())
                .active(request.getActive() != null ? request.getActive() : true)
                .moduleTaskManager(request.getModuleTaskManager() != null ? request.getModuleTaskManager() : true)
                .moduleImageManager(request.getModuleImageManager() != null ? request.getModuleImageManager() : true)
                .moduleDesigns(request.getModuleDesigns() != null ? request.getModuleDesigns() : true)
                .orgType(orgType)
                .build();
        org = organizationRepository.save(org);

        // Create first admin user if admin details provided
        if (request.getAdminUserName() != null && !request.getAdminUserName().isBlank()) {
            if (userRepository.existsByUserName(request.getAdminUserName())) {
                throw new IllegalArgumentException("Admin username already taken: " + request.getAdminUserName());
            }
            UserRole adminRole = userRoleRepository.findByName(RoleName.ADMIN)
                    .orElseThrow(() -> new RuntimeException("ADMIN role not found"));
            User admin = User.builder()
                    .userName(request.getAdminUserName())
                    .firstName(request.getAdminFirstName() != null ? request.getAdminFirstName() : "Admin")
                    .lastName(request.getAdminLastName())
                    .password(passwordEncoder.encode(request.getAdminPassword()))
                    .status(UserStatus.ACTIVE)
                    .role(adminRole)
                    .organization(org)
                    .build();
            userRepository.save(admin);
        }

        return OrganizationResponse.from(org, userRepository.countByOrganizationId(org.getId()));
    }

    public OrganizationResponse update(Long id, OrganizationRequest request) {
        Organization org = organizationRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Organization not found: " + id));

        if (!org.getName().equals(request.getName()) && organizationRepository.existsByName(request.getName())) {
            throw new IllegalArgumentException("Organization name already exists: " + request.getName());
        }
        if (!org.getSlug().equals(request.getSlug()) && organizationRepository.existsBySlug(request.getSlug())) {
            throw new IllegalArgumentException("Organization slug already exists: " + request.getSlug());
        }

        OrganizationType orgType = orgTypeRepository.findById(request.getOrgTypeId())
                .orElseThrow(() -> new EntityNotFoundException("Organization type not found: " + request.getOrgTypeId()));

        org.setName(request.getName());
        org.setSlug(request.getSlug());
        org.setOrgType(orgType);
        if (request.getActive() != null) org.setActive(request.getActive());
        if (request.getModuleTaskManager() != null) org.setModuleTaskManager(request.getModuleTaskManager());
        if (request.getModuleImageManager() != null) org.setModuleImageManager(request.getModuleImageManager());
        if (request.getModuleDesigns() != null) org.setModuleDesigns(request.getModuleDesigns());

        return OrganizationResponse.from(organizationRepository.save(org),
                userRepository.countByOrganizationId(org.getId()));
    }

    public OrganizationResponse toggleActive(Long id) {
        Organization org = organizationRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Organization not found: " + id));
        org.setActive(!org.isActive());
        return OrganizationResponse.from(organizationRepository.save(org),
                userRepository.countByOrganizationId(org.getId()));
    }
}
