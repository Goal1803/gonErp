package com.gonerp.organization.service;

import com.gonerp.organization.dto.OrganizationTypeRequest;
import com.gonerp.organization.dto.OrganizationTypeResponse;
import com.gonerp.organization.model.OrganizationType;
import com.gonerp.organization.repository.*;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class OrganizationTypeService {

    private final OrganizationTypeRepository orgTypeRepository;
    private final StaffRoleRepository staffRoleRepository;
    private final DepartmentRepository departmentRepository;
    private final UserGroupRepository userGroupRepository;

    public List<OrganizationTypeResponse> findAll() {
        return orgTypeRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    public OrganizationTypeResponse findById(Long id) {
        OrganizationType orgType = orgTypeRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Organization type not found: " + id));
        return toResponse(orgType);
    }

    public OrganizationTypeResponse create(OrganizationTypeRequest request) {
        if (orgTypeRepository.existsByName(request.getName())) {
            throw new IllegalArgumentException("Organization type name already exists: " + request.getName());
        }
        OrganizationType orgType = OrganizationType.builder()
                .name(request.getName())
                .description(request.getDescription())
                .build();
        return toResponse(orgTypeRepository.save(orgType));
    }

    public OrganizationTypeResponse update(Long id, OrganizationTypeRequest request) {
        OrganizationType orgType = orgTypeRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Organization type not found: " + id));
        orgType.setName(request.getName());
        orgType.setDescription(request.getDescription());
        return toResponse(orgTypeRepository.save(orgType));
    }

    public void delete(Long id) {
        if (!orgTypeRepository.existsById(id)) {
            throw new EntityNotFoundException("Organization type not found: " + id);
        }
        orgTypeRepository.deleteById(id);
    }

    private OrganizationTypeResponse toResponse(OrganizationType orgType) {
        long staffRoles = staffRoleRepository.findByOrgTypeId(orgType.getId()).size();
        long departments = departmentRepository.findByOrgTypeId(orgType.getId()).size();
        long userGroups = userGroupRepository.findByOrgTypeId(orgType.getId()).size();
        return OrganizationTypeResponse.from(orgType, staffRoles, departments, userGroups);
    }
}
