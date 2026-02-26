package com.gonerp.worktime.service;

import com.gonerp.usermanager.model.User;
import com.gonerp.usermanager.repository.UserRepository;
import com.gonerp.worktime.dto.BulkQuotaAssignRequest;
import com.gonerp.worktime.dto.DayOffQuotaResponse;
import com.gonerp.worktime.dto.DayOffQuotaUpdateRequest;
import com.gonerp.worktime.model.DayOffQuota;
import com.gonerp.worktime.model.DayOffType;
import com.gonerp.worktime.repository.DayOffQuotaRepository;
import com.gonerp.worktime.repository.DayOffTypeRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class DayOffQuotaService {

    private final DayOffQuotaRepository quotaRepository;
    private final DayOffTypeRepository dayOffTypeRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<DayOffQuotaResponse> getMyQuotas(Long userId, int year) {
        return quotaRepository.findByUserIdAndYear(userId, year).stream()
                .map(DayOffQuotaResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<DayOffQuotaResponse> getUserQuotas(Long userId, int year) {
        return quotaRepository.findByUserIdAndYear(userId, year).stream()
                .map(DayOffQuotaResponse::from)
                .toList();
    }

    public DayOffQuotaResponse setQuota(Long userId, Long typeId, DayOffQuotaUpdateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + userId));
        DayOffType dayOffType = dayOffTypeRepository.findById(typeId)
                .orElseThrow(() -> new EntityNotFoundException("Day-off type not found: " + typeId));

        int year = java.time.LocalDate.now().getYear();

        DayOffQuota quota = quotaRepository.findByUserIdAndDayOffTypeIdAndYear(userId, typeId, year)
                .orElseGet(() -> DayOffQuota.builder()
                        .user(user)
                        .dayOffType(dayOffType)
                        .year(year)
                        .build());

        if (request.getTotalDays() != null) {
            quota.setTotalDays(request.getTotalDays());
        }
        if (request.getCarriedOverDays() != null) {
            quota.setCarriedOverDays(request.getCarriedOverDays());
        }

        return DayOffQuotaResponse.from(quotaRepository.save(quota));
    }

    public List<DayOffQuotaResponse> bulkAssignDefaults(Long orgId, BulkQuotaAssignRequest request) {
        DayOffType dayOffType = dayOffTypeRepository.findById(request.getDayOffTypeId())
                .orElseThrow(() -> new EntityNotFoundException("Day-off type not found: " + request.getDayOffTypeId()));

        // Validate that the day-off type belongs to the organization
        if (!dayOffType.getOrganization().getId().equals(orgId)) {
            throw new IllegalArgumentException("Day-off type does not belong to the organization");
        }

        List<User> users = userRepository.findByOrganizationId(orgId);

        for (User user : users) {
            DayOffQuota quota = quotaRepository
                    .findByUserIdAndDayOffTypeIdAndYear(user.getId(), request.getDayOffTypeId(), request.getYear())
                    .orElseGet(() -> DayOffQuota.builder()
                            .user(user)
                            .dayOffType(dayOffType)
                            .year(request.getYear())
                            .build());

            quota.setTotalDays(request.getTotalDays());
            quotaRepository.save(quota);
        }

        return quotaRepository.findByDayOffTypeOrganizationIdAndYear(orgId, request.getYear()).stream()
                .map(DayOffQuotaResponse::from)
                .toList();
    }
}
