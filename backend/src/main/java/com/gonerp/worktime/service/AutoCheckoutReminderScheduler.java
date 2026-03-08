package com.gonerp.worktime.service;

import com.gonerp.worktime.event.WorkTimeNotificationEvent;
import com.gonerp.worktime.model.TimeEntry;
import com.gonerp.worktime.model.WorkTimeSettings;
import com.gonerp.worktime.model.enums.TimeEntryStatus;
import com.gonerp.worktime.repository.TimeEntryRepository;
import com.gonerp.worktime.repository.WorkTimeSettingsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class AutoCheckoutReminderScheduler {

    private final TimeEntryRepository timeEntryRepository;
    private final WorkTimeSettingsRepository settingsRepository;
    private final ApplicationEventPublisher eventPublisher;

    // Track which entries already got a reminder today (reset daily)
    private final Set<Long> remindedToday = ConcurrentHashMap.newKeySet();
    private LocalDate lastResetDate = LocalDate.now();

    @Scheduled(fixedRate = 300000) // every 5 minutes
    public void checkForAutoCheckoutReminders() {
        // Reset reminder tracking daily (using system default zone for the reset check)
        LocalDate sysToday = LocalDate.now();
        if (!sysToday.equals(lastResetDate)) {
            remindedToday.clear();
            lastResetDate = sysToday;
        }

        List<WorkTimeSettings> allSettings = settingsRepository.findAll();
        for (WorkTimeSettings settings : allSettings) {
            if (settings.getAutoCheckoutReminderMinutes() <= 0) continue;

            ZoneId zoneId = settings.getZoneId();
            LocalDate today = LocalDate.now(zoneId);
            Long orgId = settings.getOrganization().getId();
            List<TimeEntry> activeEntries = timeEntryRepository.findByOrganizationIdAndWorkDate(orgId, today);

            OffsetDateTime now = ZonedDateTime.now(zoneId).toOffsetDateTime();
            for (TimeEntry entry : activeEntries) {
                if (entry.getStatus() == TimeEntryStatus.CHECKED_OUT) continue;
                if (remindedToday.contains(entry.getId())) continue;

                long minutesSinceCheckIn = Duration.between(entry.getCheckInTime(), now).toMinutes();
                long expectedMinutes = (long) (settings.getDailyWorkingHours() * 60);

                if (minutesSinceCheckIn >= expectedMinutes + settings.getAutoCheckoutReminderMinutes()) {
                    remindedToday.add(entry.getId());
                    eventPublisher.publishEvent(new WorkTimeNotificationEvent(
                            entry.getUser().getId(),
                            Set.of(entry.getUser().getId()),
                            "AUTO_CHECKOUT_REMINDER",
                            "You've been checked in for over " + (minutesSinceCheckIn / 60) + " hours. Don't forget to check out!"
                    ));
                    log.info("Sent auto-checkout reminder to user {} (entry {})",
                            entry.getUser().getUserName(), entry.getId());
                }
            }
        }
    }
}
