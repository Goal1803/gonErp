package com.gonerp.worktime.service;

import com.gonerp.organization.model.Organization;
import com.gonerp.organization.repository.OrganizationRepository;
import com.gonerp.worktime.dto.DayOffTypeRequest;
import com.gonerp.worktime.dto.DayOffTypeResponse;
import com.gonerp.worktime.model.DayOffType;
import com.gonerp.worktime.repository.DayOffTypeRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class DayOffTypeService {

    private final DayOffTypeRepository dayOffTypeRepository;
    private final OrganizationRepository organizationRepository;

    @Transactional(readOnly = true)
    public List<DayOffTypeResponse> getAllForOrg(Long orgId) {
        return dayOffTypeRepository.findByOrganizationIdOrderByDisplayOrder(orgId).stream()
                .map(DayOffTypeResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<DayOffTypeResponse> getActiveForOrg(Long orgId) {
        return dayOffTypeRepository.findByOrganizationIdAndActiveOrderByDisplayOrder(orgId, true).stream()
                .map(DayOffTypeResponse::from)
                .toList();
    }

    public DayOffTypeResponse create(Long orgId, DayOffTypeRequest request) {
        Organization org = organizationRepository.findById(orgId)
                .orElseThrow(() -> new EntityNotFoundException("Organization not found: " + orgId));

        DayOffType type = DayOffType.builder()
                .organization(org)
                .name(request.getName())
                .color(request.getColor() != null ? request.getColor() : "#42A5F5")
                .isPaid(request.getIsPaid() != null ? request.getIsPaid() : true)
                .defaultQuota(request.getDefaultQuota() != null ? request.getDefaultQuota() : 20.0)
                .displayOrder(request.getDisplayOrder() != null ? request.getDisplayOrder() : 0)
                .active(request.getActive() != null ? request.getActive() : true)
                .build();

        return DayOffTypeResponse.from(dayOffTypeRepository.save(type));
    }

    public DayOffTypeResponse update(Long id, DayOffTypeRequest request) {
        DayOffType type = dayOffTypeRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Day-off type not found: " + id));

        type.setName(request.getName());
        if (request.getColor() != null) type.setColor(request.getColor());
        if (request.getIsPaid() != null) type.setPaid(request.getIsPaid());
        if (request.getDefaultQuota() != null) type.setDefaultQuota(request.getDefaultQuota());
        if (request.getDisplayOrder() != null) type.setDisplayOrder(request.getDisplayOrder());
        if (request.getActive() != null) type.setActive(request.getActive());

        return DayOffTypeResponse.from(dayOffTypeRepository.save(type));
    }

    public void delete(Long id) {
        if (!dayOffTypeRepository.existsById(id)) {
            throw new EntityNotFoundException("Day-off type not found: " + id);
        }
        dayOffTypeRepository.deleteById(id);
    }
}
