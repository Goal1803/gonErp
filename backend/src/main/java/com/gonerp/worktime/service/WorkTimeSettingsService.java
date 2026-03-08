package com.gonerp.worktime.service;

import com.gonerp.organization.model.Organization;
import com.gonerp.organization.repository.OrganizationRepository;
import com.gonerp.worktime.dto.WorkTimeSettingsRequest;
import com.gonerp.worktime.dto.WorkTimeSettingsResponse;
import com.gonerp.worktime.model.WorkTimeSettings;
import com.gonerp.worktime.repository.WorkTimeSettingsRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneId;

@Service
@RequiredArgsConstructor
@Transactional
public class WorkTimeSettingsService {

    private final WorkTimeSettingsRepository settingsRepository;
    private final OrganizationRepository organizationRepository;

    @Transactional(readOnly = true)
    public WorkTimeSettingsResponse getSettings(Long organizationId) {
        WorkTimeSettings settings = settingsRepository.findByOrganizationId(organizationId)
                .orElseGet(() -> createDefaultSettings(organizationId));
        return WorkTimeSettingsResponse.from(settings);
    }

    public WorkTimeSettingsResponse updateSettings(Long organizationId, WorkTimeSettingsRequest request) {
        WorkTimeSettings settings = settingsRepository.findByOrganizationId(organizationId)
                .orElseGet(() -> createDefaultSettings(organizationId));

        if (request.getDailyWorkingHours() != null) settings.setDailyWorkingHours(request.getDailyWorkingHours());
        if (request.getWeeklyFullTimeHours() != null) settings.setWeeklyFullTimeHours(request.getWeeklyFullTimeHours());
        if (request.getWeeklyPartTimeHours() != null) settings.setWeeklyPartTimeHours(request.getWeeklyPartTimeHours());
        if (request.getRequiredBreakMinutes() != null) settings.setRequiredBreakMinutes(request.getRequiredBreakMinutes());
        if (request.getBreakCountsAsWork() != null) settings.setBreakCountsAsWork(request.getBreakCountsAsWork());
        if (request.getOvertimeTrackingEnabled() != null) settings.setOvertimeTrackingEnabled(request.getOvertimeTrackingEnabled());
        if (request.getLateEarlyTrackingEnabled() != null) settings.setLateEarlyTrackingEnabled(request.getLateEarlyTrackingEnabled());
        if (request.getWorkStartTime() != null) settings.setWorkStartTime(request.getWorkStartTime());
        if (request.getWorkEndTime() != null) settings.setWorkEndTime(request.getWorkEndTime());
        if (request.getCarryOverEnabled() != null) settings.setCarryOverEnabled(request.getCarryOverEnabled());
        if (request.getMaxCarryOverDays() != null) settings.setMaxCarryOverDays(request.getMaxCarryOverDays());
        if (request.getDailyNotesEnabled() != null) settings.setDailyNotesEnabled(request.getDailyNotesEnabled());
        if (request.getWorkLocationEnabled() != null) settings.setWorkLocationEnabled(request.getWorkLocationEnabled());
        if (request.getAutoCheckoutReminderMinutes() != null) settings.setAutoCheckoutReminderMinutes(request.getAutoCheckoutReminderMinutes());
        if (request.getTimezoneId() != null) {
            ZoneId.of(request.getTimezoneId()); // validate — throws DateTimeException if invalid
            settings.setTimezoneId(request.getTimezoneId());
        }

        return WorkTimeSettingsResponse.from(settingsRepository.save(settings));
    }

    private WorkTimeSettings createDefaultSettings(Long organizationId) {
        Organization org = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new EntityNotFoundException("Organization not found: " + organizationId));
        WorkTimeSettings settings = WorkTimeSettings.builder().organization(org).build();
        return settingsRepository.save(settings);
    }
}
