package com.gonerp.worktime.service;

import com.gonerp.worktime.event.WorkTimeNotificationEvent;
import com.gonerp.worktime.model.TimeEntry;
import com.gonerp.worktime.model.UserWorkTimeConfig;
import com.gonerp.worktime.model.WorkTimeSettings;
import com.gonerp.worktime.model.enums.TimeEntryStatus;
import com.gonerp.worktime.repository.TimeEntryRepository;
import com.gonerp.worktime.repository.WorkTimeSettingsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
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
    private final TimeClockService timeClockService;
    private final UserWorkTimeConfigService userConfigService;

    // Track which entries already got a reminder today (reset daily)
    private final Set<Long> remindedToday = ConcurrentHashMap.newKeySet();
    private LocalDate lastResetDate = LocalDate.now();

    @Transactional
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

            Long orgId = settings.getOrganization().getId();

            // Check entries for today in system timezone (broad sweep)
            List<TimeEntry> activeEntries = timeEntryRepository.findByOrganizationIdAndWorkDate(orgId, sysToday);

            for (TimeEntry entry : activeEntries) {
                if (entry.getStatus() == TimeEntryStatus.CHECKED_OUT) continue;
                if (remindedToday.contains(entry.getId())) continue;

                // Use per-user config for timezone and daily hours
                UserWorkTimeConfig userConfig = userConfigService.getOrCreateConfig(entry.getUser());
                ZoneId userZoneId = userConfig.getZoneId();
                OffsetDateTime now = ZonedDateTime.now(userZoneId).toOffsetDateTime();

                long minutesSinceCheckIn = Duration.between(entry.getCheckInTime(), now).toMinutes();
                long expectedMinutes = (long) (userConfig.getDailyWorkingHours() * 60);

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

    @Transactional
    @Scheduled(fixedRate = 60000) // every minute
    public void checkForMidnightForceCheckout() {
        // Find all active entries (not yet checked out) across all orgs
        List<TimeEntryStatus> activeStatuses = List.of(TimeEntryStatus.CHECKED_IN, TimeEntryStatus.ON_BREAK);

        List<WorkTimeSettings> allSettings = settingsRepository.findAll();
        for (WorkTimeSettings settings : allSettings) {
            Long orgId = settings.getOrganization().getId();

            // Check the last 2 days to cover all timezone scenarios
            LocalDate sysToday = LocalDate.now();
            for (int daysBack = 0; daysBack <= 1; daysBack++) {
                LocalDate checkDate = sysToday.minusDays(daysBack);
                List<TimeEntry> activeEntries = timeEntryRepository.findByOrganizationIdAndWorkDateAndStatusIn(
                        orgId, checkDate, activeStatuses);

                LocalTime forceCheckoutTime = settings.getForceCheckoutTime() != null
                        ? settings.getForceCheckoutTime() : LocalTime.of(0, 0);

                for (TimeEntry entry : activeEntries) {
                    try {
                        UserWorkTimeConfig userConfig = userConfigService.getOrCreateConfig(entry.getUser());
                        ZoneId userZoneId = userConfig.getZoneId();
                        ZonedDateTime nowInUserTz = ZonedDateTime.now(userZoneId);
                        LocalDate userToday = nowInUserTz.toLocalDate();
                        LocalTime userNow = nowInUserTz.toLocalTime();

                        // Force checkout when the configured time has passed on a day after the entry's workDate
                        boolean shouldForceCheckout;
                        if (forceCheckoutTime.equals(LocalTime.of(0, 0))) {
                            // Midnight: force checkout when the date has changed
                            shouldForceCheckout = userToday.isAfter(entry.getWorkDate());
                        } else {
                            // Custom time: force checkout when past that time on a later day
                            shouldForceCheckout = userToday.isAfter(entry.getWorkDate())
                                    && !userNow.isBefore(forceCheckoutTime);
                        }

                        if (shouldForceCheckout) {
                            timeClockService.forceCheckoutAtMidnight(entry);
                            eventPublisher.publishEvent(new WorkTimeNotificationEvent(
                                    entry.getUser().getId(),
                                    Set.of(entry.getUser().getId()),
                                    "FORCE_CHECKOUT_MIDNIGHT",
                                    "You were automatically checked out. Time entries cannot span two days."
                            ));
                            log.info("Force checkout for user {} (entry {}, tz={}, forceTime={})",
                                    entry.getUser().getUserName(), entry.getId(), userConfig.getTimezoneId(), forceCheckoutTime);
                        }
                    } catch (Exception e) {
                        log.error("Failed to force checkout user {} at configured time", entry.getUser().getUserName(), e);
                    }
                }
            }
        }
    }
}
