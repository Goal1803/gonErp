package com.gonerp.worktime.service;

import com.gonerp.usermanager.model.User;
import com.gonerp.usermanager.repository.UserRepository;
import com.gonerp.worktime.dto.*;
import com.gonerp.worktime.model.BreakEntry;
import com.gonerp.worktime.model.TimeEntry;
import com.gonerp.worktime.model.UserWorkTimeConfig;
import com.gonerp.worktime.model.WorkTimeSettings;
import com.gonerp.worktime.model.enums.TimeEntryStatus;
import com.gonerp.worktime.model.enums.WorkLocation;
import com.gonerp.worktime.repository.BreakEntryRepository;
import com.gonerp.worktime.repository.TimeEntryRepository;
import com.gonerp.worktime.repository.WorkTimeSettingsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class TimeClockService {

    private final TimeEntryRepository timeEntryRepository;
    private final BreakEntryRepository breakEntryRepository;
    private final WorkTimeSettingsRepository settingsRepository;
    private final UserRepository userRepository;
    private final UserWorkTimeConfigService userConfigService;

    // ── Check In ────────────────────────────────────────────────────────────────

    public TimeEntryResponse checkIn(CheckInRequest request) {
        User user = getCurrentUser();
        UserWorkTimeConfig userConfig = userConfigService.getOrCreateConfig(user);
        ZoneId zoneId = userConfig.getZoneId();
        LocalDate today = LocalDate.now(zoneId);

        // Ensure no existing active entry for today
        Optional<TimeEntry> existing = timeEntryRepository.findByUserIdAndWorkDate(user.getId(), today);
        if (existing.isPresent()) {
            TimeEntry entry = existing.get();
            if (entry.getStatus() == TimeEntryStatus.CHECKED_IN || entry.getStatus() == TimeEntryStatus.ON_BREAK) {
                throw new IllegalStateException("Already checked in for today");
            }
            if (entry.getStatus() == TimeEntryStatus.CHECKED_OUT) {
                throw new IllegalStateException("Already checked out for today. Cannot check in again.");
            }
        }

        OffsetDateTime now = ZonedDateTime.now(zoneId).toOffsetDateTime();

        // Parse work location from request
        WorkLocation workLocation = null;
        if (request != null && request.getWorkLocation() != null && !request.getWorkLocation().isBlank()) {
            try {
                workLocation = WorkLocation.valueOf(request.getWorkLocation().toUpperCase());
            } catch (IllegalArgumentException e) {
                // Ignore invalid work location, leave as null
            }
        }

        // Detect late arrival based on user config
        boolean lateArrival = false;
        WorkTimeSettings settings = getSettingsForUser(user);
        if (settings != null && settings.isLateEarlyTrackingEnabled()) {
            if (now.atZoneSameInstant(zoneId).toLocalTime().isAfter(userConfig.getWorkStartTime())) {
                lateArrival = true;
            }
        }

        TimeEntry entry = TimeEntry.builder()
                .user(user)
                .organization(user.getOrganization())
                .workDate(today)
                .checkInTime(now)
                .status(TimeEntryStatus.CHECKED_IN)
                .workLocation(workLocation)
                .isLateArrival(lateArrival)
                .build();

        entry = timeEntryRepository.save(entry);
        return TimeEntryResponse.from(entry);
    }

    // ── Pause (start break) ─────────────────────────────────────────────────────

    public TimeEntryResponse pause() {
        User user = getCurrentUser();
        TimeEntry entry = getTodayEntryOrThrow(user);

        if (entry.getStatus() != TimeEntryStatus.CHECKED_IN) {
            throw new IllegalStateException("Cannot start break: current status is " + entry.getStatus());
        }

        ZoneId zoneId = getUserZoneId(user);
        OffsetDateTime now = ZonedDateTime.now(zoneId).toOffsetDateTime();

        // Create a new break entry
        BreakEntry breakEntry = BreakEntry.builder()
                .timeEntry(entry)
                .startTime(now)
                .build();
        entry.getBreaks().add(breakEntry);
        entry.setStatus(TimeEntryStatus.ON_BREAK);

        entry = timeEntryRepository.save(entry);
        return TimeEntryResponse.from(entry);
    }

    // ── Resume (end break) ──────────────────────────────────────────────────────

    public TimeEntryResponse resume() {
        User user = getCurrentUser();
        TimeEntry entry = getTodayEntryOrThrow(user);

        if (entry.getStatus() != TimeEntryStatus.ON_BREAK) {
            throw new IllegalStateException("Cannot resume: current status is " + entry.getStatus());
        }

        ZoneId zoneId = getUserZoneId(user);
        OffsetDateTime now = ZonedDateTime.now(zoneId).toOffsetDateTime();

        // Find the open break (endTime is null) and close it
        BreakEntry openBreak = entry.getBreaks().stream()
                .filter(b -> b.getEndTime() == null)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No open break found"));

        openBreak.setEndTime(now);
        int breakDuration = (int) Duration.between(openBreak.getStartTime(), now).toMinutes();
        openBreak.setDurationMinutes(breakDuration);

        // Update total break minutes on the time entry
        entry.setTotalBreakMinutes(entry.getTotalBreakMinutes() + breakDuration);
        entry.setStatus(TimeEntryStatus.CHECKED_IN);

        entry = timeEntryRepository.save(entry);
        return TimeEntryResponse.from(entry);
    }

    // ── Check Out ───────────────────────────────────────────────────────────────

    public TimeEntryResponse checkOut(CheckOutRequest request) {
        User user = getCurrentUser();
        TimeEntry entry = getTodayEntryOrThrow(user);

        if (entry.getStatus() == TimeEntryStatus.CHECKED_OUT) {
            throw new IllegalStateException("Already checked out for today");
        }

        UserWorkTimeConfig userConfig = userConfigService.getOrCreateConfig(user);
        ZoneId zoneId = userConfig.getZoneId();
        OffsetDateTime now = ZonedDateTime.now(zoneId).toOffsetDateTime();

        // If currently on break, close the open break first
        if (entry.getStatus() == TimeEntryStatus.ON_BREAK) {
            BreakEntry openBreak = entry.getBreaks().stream()
                    .filter(b -> b.getEndTime() == null)
                    .findFirst()
                    .orElse(null);
            if (openBreak != null) {
                openBreak.setEndTime(now);
                int breakDuration = (int) Duration.between(openBreak.getStartTime(), now).toMinutes();
                openBreak.setDurationMinutes(breakDuration);
                entry.setTotalBreakMinutes(entry.getTotalBreakMinutes() + breakDuration);
            }
        }

        // Compute total work minutes
        long totalMinutes = Duration.between(entry.getCheckInTime(), now).toMinutes();
        int breakMins = entry.getTotalBreakMinutes();

        // Check if break counts as work from org settings
        WorkTimeSettings settings = getSettingsForUser(user);
        boolean breakCountsAsWork = settings != null && settings.isBreakCountsAsWork();
        int workMinutes = (int) totalMinutes - (breakCountsAsWork ? 0 : breakMins);
        if (workMinutes < 0) workMinutes = 0;

        entry.setCheckOutTime(now);
        entry.setTotalWorkMinutes(workMinutes);
        entry.setStatus(TimeEntryStatus.CHECKED_OUT);

        // Set daily notes from request
        if (request != null && request.getDailyNotes() != null) {
            entry.setDailyNotes(request.getDailyNotes());
        }

        // Detect early departure using user config
        if (settings != null && settings.isLateEarlyTrackingEnabled()) {
            if (now.atZoneSameInstant(zoneId).toLocalTime().isBefore(userConfig.getWorkEndTime())) {
                entry.setEarlyDeparture(true);
            }
        }

        // Compute overtime using user config
        if (settings != null && settings.isOvertimeTrackingEnabled()) {
            int dailyExpectedMinutes = (int) (userConfig.getDailyWorkingHours() * 60);
            if (workMinutes > dailyExpectedMinutes) {
                entry.setOvertimeMinutes(workMinutes - dailyExpectedMinutes);
            }
        }

        entry = timeEntryRepository.save(entry);
        return TimeEntryResponse.from(entry);
    }

    // ── Force Checkout at Midnight ─────────────────────────────────────────────

    public void forceCheckoutAtMidnight(TimeEntry entry) {
        forceCheckoutAt(entry, LocalTime.of(0, 0));
    }

    public void forceCheckoutAt(TimeEntry entry, LocalTime forceTime) {
        if (entry.getStatus() == TimeEntryStatus.CHECKED_OUT) return;

        UserWorkTimeConfig userConfig = userConfigService.getOrCreateConfig(entry.getUser());
        ZoneId zoneId = userConfig.getZoneId();

        // Determine the checkout timestamp in the user's timezone
        OffsetDateTime checkoutTime;
        String noteLabel;
        if (forceTime.equals(LocalTime.of(0, 0))) {
            // Midnight = start of next day in user's timezone
            checkoutTime = entry.getWorkDate().plusDays(1)
                    .atStartOfDay(zoneId)
                    .toOffsetDateTime();
            noteLabel = "[Auto-checked out at midnight]";
        } else {
            // Configured time on the same work date in user's timezone
            checkoutTime = entry.getWorkDate()
                    .atTime(forceTime)
                    .atZone(zoneId)
                    .toOffsetDateTime();
            noteLabel = "[Auto-checked out at " + forceTime + "]";
        }

        // Don't set checkout before check-in
        if (checkoutTime.isBefore(entry.getCheckInTime())) {
            checkoutTime = entry.getCheckInTime().plusMinutes(1);
        }

        // Close any open break at the checkout time
        if (entry.getStatus() == TimeEntryStatus.ON_BREAK) {
            BreakEntry openBreak = entry.getBreaks().stream()
                    .filter(b -> b.getEndTime() == null)
                    .findFirst()
                    .orElse(null);
            if (openBreak != null) {
                openBreak.setEndTime(checkoutTime);
                int breakDuration = (int) Duration.between(openBreak.getStartTime(), checkoutTime).toMinutes();
                openBreak.setDurationMinutes(Math.max(0, breakDuration));
                entry.setTotalBreakMinutes(entry.getTotalBreakMinutes() + Math.max(0, breakDuration));
            }
        }

        // Compute total work minutes
        long totalMinutes = Duration.between(entry.getCheckInTime(), checkoutTime).toMinutes();
        int breakMins = entry.getTotalBreakMinutes();

        WorkTimeSettings settings = getSettingsForUser(entry.getUser());
        boolean breakCountsAsWork = settings != null && settings.isBreakCountsAsWork();
        int workMinutes = (int) totalMinutes - (breakCountsAsWork ? 0 : breakMins);
        if (workMinutes < 0) workMinutes = 0;

        entry.setCheckOutTime(checkoutTime);
        entry.setTotalWorkMinutes(workMinutes);
        entry.setStatus(TimeEntryStatus.CHECKED_OUT);

        // Detect early departure using user config
        if (settings != null && settings.isLateEarlyTrackingEnabled()) {
            if (checkoutTime.atZoneSameInstant(zoneId).toLocalTime().isBefore(userConfig.getWorkEndTime())) {
                entry.setEarlyDeparture(true);
            }
        }

        // Compute overtime using user config
        if (settings != null && settings.isOvertimeTrackingEnabled()) {
            int dailyExpectedMinutes = (int) (userConfig.getDailyWorkingHours() * 60);
            if (workMinutes > dailyExpectedMinutes) {
                entry.setOvertimeMinutes(workMinutes - dailyExpectedMinutes);
            }
        }

        // Append note
        String note = entry.getDailyNotes() != null ? entry.getDailyNotes() + "\n" : "";
        entry.setDailyNotes(note + noteLabel);

        timeEntryRepository.save(entry);
    }

    // ── Get Status ──────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public ClockStatusResponse getStatus() {
        User user = getCurrentUser();
        ZoneId zoneId = getUserZoneId(user);
        LocalDate today = LocalDate.now(zoneId);
        Optional<TimeEntry> optEntry = timeEntryRepository.findByUserIdAndWorkDate(user.getId(), today);

        if (optEntry.isEmpty()) {
            return ClockStatusResponse.builder()
                    .status(null)
                    .isClockedIn(false)
                    .isOnBreak(false)
                    .build();
        }

        TimeEntry entry = optEntry.get();
        OffsetDateTime now = ZonedDateTime.now(zoneId).toOffsetDateTime();

        boolean isClockedIn = entry.getStatus() == TimeEntryStatus.CHECKED_IN
                || entry.getStatus() == TimeEntryStatus.ON_BREAK;
        boolean isOnBreak = entry.getStatus() == TimeEntryStatus.ON_BREAK;

        // Compute elapsed work and break minutes in real time
        int elapsedBreakMinutes = entry.getTotalBreakMinutes();
        int elapsedWorkMinutes = 0;

        if (isClockedIn && entry.getCheckInTime() != null) {
            long totalElapsed = Duration.between(entry.getCheckInTime(), now).toMinutes();

            // If on break, add the current open break duration to elapsed break
            if (isOnBreak) {
                BreakEntry openBreak = entry.getBreaks().stream()
                        .filter(b -> b.getEndTime() == null)
                        .findFirst()
                        .orElse(null);
                if (openBreak != null) {
                    elapsedBreakMinutes += (int) Duration.between(openBreak.getStartTime(), now).toMinutes();
                }
            }

            elapsedWorkMinutes = (int) totalElapsed - elapsedBreakMinutes;
            if (elapsedWorkMinutes < 0) elapsedWorkMinutes = 0;
        } else if (entry.getStatus() == TimeEntryStatus.CHECKED_OUT) {
            elapsedWorkMinutes = entry.getTotalWorkMinutes();
            elapsedBreakMinutes = entry.getTotalBreakMinutes();
        }

        return ClockStatusResponse.builder()
                .status(entry.getStatus() != null ? entry.getStatus().name() : null)
                .isClockedIn(isClockedIn)
                .isOnBreak(isOnBreak)
                .currentEntryId(entry.getId())
                .checkInTime(entry.getCheckInTime())
                .workLocation(entry.getWorkLocation() != null ? entry.getWorkLocation().name() : null)
                .elapsedWorkMinutes(elapsedWorkMinutes)
                .elapsedBreakMinutes(elapsedBreakMinutes)
                .build();
    }

    // ── Get Today ───────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public TimeEntryResponse getToday() {
        User user = getCurrentUser();
        ZoneId zoneId = getUserZoneId(user);
        LocalDate today = LocalDate.now(zoneId);
        return timeEntryRepository.findByUserIdAndWorkDate(user.getId(), today)
                .map(TimeEntryResponse::from)
                .orElse(null);
    }

    // ── Helpers ─────────────────────────────────────────────────────────────────

    private User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUserName(username)
                .orElseThrow(() -> new IllegalStateException("Current user not found: " + username));
        if (user.getOrganization() == null) {
            throw new IllegalStateException("Cannot use time clock: your account is not assigned to an organization.");
        }
        return user;
    }

    private TimeEntry getTodayEntryOrThrow(User user) {
        ZoneId zoneId = getUserZoneId(user);
        LocalDate today = LocalDate.now(zoneId);
        return timeEntryRepository.findByUserIdAndWorkDate(user.getId(), today)
                .orElseThrow(() -> new IllegalStateException("No time entry found for today. Please check in first."));
    }

    private ZoneId getUserZoneId(User user) {
        UserWorkTimeConfig config = userConfigService.getOrCreateConfig(user);
        return config.getZoneId();
    }

    private WorkTimeSettings getSettingsForUser(User user) {
        if (user.getOrganization() == null) {
            return null;
        }
        return settingsRepository.findByOrganizationId(user.getOrganization().getId()).orElse(null);
    }
}
