package com.gonerp.worktime.service;

import com.gonerp.organization.model.Organization;
import com.gonerp.organization.repository.OrganizationRepository;
import com.gonerp.worktime.dto.PublicHolidayRequest;
import com.gonerp.worktime.dto.PublicHolidayResponse;
import com.gonerp.worktime.model.PublicHoliday;
import com.gonerp.worktime.repository.PublicHolidayRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class PublicHolidayService {

    private final PublicHolidayRepository publicHolidayRepository;
    private final OrganizationRepository organizationRepository;

    @Transactional(readOnly = true)
    public List<PublicHolidayResponse> getAll(Long orgId) {
        return publicHolidayRepository.findByOrganizationId(orgId).stream()
                .map(PublicHolidayResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PublicHolidayResponse> getForDateRange(Long orgId, LocalDate start, LocalDate end) {
        return publicHolidayRepository.findByOrganizationIdAndHolidayDateBetween(orgId, start, end).stream()
                .map(PublicHolidayResponse::from)
                .toList();
    }

    public PublicHolidayResponse create(Long orgId, PublicHolidayRequest request) {
        Organization org = organizationRepository.findById(orgId)
                .orElseThrow(() -> new EntityNotFoundException("Organization not found: " + orgId));

        PublicHoliday holiday = PublicHoliday.builder()
                .organization(org)
                .holidayDate(request.getHolidayDate())
                .name(request.getName())
                .isRecurring(request.getIsRecurring() != null ? request.getIsRecurring() : false)
                .build();

        return PublicHolidayResponse.from(publicHolidayRepository.save(holiday));
    }

    public PublicHolidayResponse update(Long id, PublicHolidayRequest request) {
        PublicHoliday holiday = publicHolidayRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Public holiday not found: " + id));

        holiday.setHolidayDate(request.getHolidayDate());
        holiday.setName(request.getName());
        if (request.getIsRecurring() != null) {
            holiday.setRecurring(request.getIsRecurring());
        }

        return PublicHolidayResponse.from(publicHolidayRepository.save(holiday));
    }

    public void delete(Long id) {
        if (!publicHolidayRepository.existsById(id)) {
            throw new EntityNotFoundException("Public holiday not found: " + id);
        }
        publicHolidayRepository.deleteById(id);
    }
}
