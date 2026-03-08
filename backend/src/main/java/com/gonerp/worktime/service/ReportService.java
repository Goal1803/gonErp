package com.gonerp.worktime.service;

import com.gonerp.usermanager.model.User;
import com.gonerp.usermanager.repository.UserRepository;
import com.gonerp.worktime.dto.*;
import com.gonerp.worktime.model.BreakEntry;
import com.gonerp.worktime.model.DayOffRequest;
import com.gonerp.worktime.model.TimeEntry;
import com.gonerp.worktime.model.UserWorkTimeConfig;
import com.gonerp.worktime.model.WorkTimeSettings;
import com.gonerp.worktime.model.enums.DayOffRequestStatus;
import com.gonerp.worktime.model.enums.TimeEntryStatus;
import com.gonerp.worktime.repository.DayOffRequestRepository;
import com.gonerp.worktime.repository.TimeEntryRepository;
import com.gonerp.worktime.repository.WorkTimeSettingsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportService {

    private final TimeEntryRepository timeEntryRepository;
    private final DayOffRequestRepository dayOffRequestRepository;
    private final UserRepository userRepository;
    private final WorkTimeSettingsRepository settingsRepository;
    private final UserWorkTimeConfigService userConfigService;

    // ── My Daily Report ─────────────────────────────────────────────────────────

    public DailyReportDTO getDailyReport(Long userId, LocalDate date) {
        return timeEntryRepository.findByUserIdAndWorkDate(userId, date)
                .map(this::toDailyReportDTO)
                .orElse(DailyReportDTO.builder()
                        .date(date)
                        .totalWorkMinutes(0)
                        .totalBreakMinutes(0)
                        .overtimeMinutes(0)
                        .build());
    }

    // ── My Weekly Report ────────────────────────────────────────────────────────

    public WeeklyReportDTO getWeeklyReport(Long userId, LocalDate weekStart) {
        LocalDate weekEnd = weekStart.plusDays(6);
        List<TimeEntry> entries = timeEntryRepository.findByUserIdAndWorkDateBetweenOrderByWorkDateDesc(
                userId, weekStart, weekEnd);

        List<DailyReportDTO> dailyEntries = entries.stream()
                .map(this::toDailyReportDTO)
                .toList();

        int totalWork = entries.stream().mapToInt(TimeEntry::getTotalWorkMinutes).sum();
        int totalBreak = entries.stream().mapToInt(TimeEntry::getTotalBreakMinutes).sum();
        int totalOvertime = entries.stream().mapToInt(TimeEntry::getOvertimeMinutes).sum();
        int daysWorked = entries.size();
        int avgWork = daysWorked > 0 ? totalWork / daysWorked : 0;

        return WeeklyReportDTO.builder()
                .weekStart(weekStart)
                .weekEnd(weekEnd)
                .totalWorkMinutes(totalWork)
                .totalBreakMinutes(totalBreak)
                .totalOvertimeMinutes(totalOvertime)
                .daysWorked(daysWorked)
                .dailyEntries(dailyEntries)
                .averageWorkMinutesPerDay(avgWork)
                .build();
    }

    // ── My Monthly Report ───────────────────────────────────────────────────────

    public MonthlyReportDTO getMonthlyReport(Long userId, int year, int month) {
        YearMonth ym = YearMonth.of(year, month);
        LocalDate monthStart = ym.atDay(1);
        LocalDate monthEnd = ym.atEndOfMonth();

        List<TimeEntry> entries = timeEntryRepository.findByUserIdAndWorkDateBetweenOrderByWorkDateDesc(
                userId, monthStart, monthEnd);

        int totalWork = entries.stream().mapToInt(TimeEntry::getTotalWorkMinutes).sum();
        int totalBreak = entries.stream().mapToInt(TimeEntry::getTotalBreakMinutes).sum();
        int totalOvertime = entries.stream().mapToInt(TimeEntry::getOvertimeMinutes).sum();
        int daysWorked = entries.size();
        int lateArrivals = (int) entries.stream().filter(TimeEntry::isLateArrival).count();
        int earlyDepartures = (int) entries.stream().filter(TimeEntry::isEarlyDeparture).count();

        // Count days off from approved DayOffRequests
        List<DayOffRequest> dayOffRequests = dayOffRequestRepository.findByUserIdAndStartDateBetween(
                userId, monthStart, monthEnd);
        int daysOff = (int) dayOffRequests.stream()
                .filter(r -> r.getStatus() == DayOffRequestStatus.APPROVED)
                .mapToDouble(DayOffRequest::getTotalDays)
                .sum();

        // Build weekly breakdown
        List<WeeklyReportDTO> weeklyBreakdown = buildWeeklyBreakdown(entries, monthStart, monthEnd);

        return MonthlyReportDTO.builder()
                .year(year)
                .month(month)
                .totalWorkMinutes(totalWork)
                .totalBreakMinutes(totalBreak)
                .totalOvertimeMinutes(totalOvertime)
                .daysWorked(daysWorked)
                .daysOff(daysOff)
                .lateArrivals(lateArrivals)
                .earlyDepartures(earlyDepartures)
                .weeklyBreakdown(weeklyBreakdown)
                .build();
    }

    // ── Team Daily Report ───────────────────────────────────────────────────────

    public TeamDailyReportDTO getTeamDailyReport(Long orgId, LocalDate date) {
        List<TimeEntry> entries = timeEntryRepository.findByOrganizationIdAndWorkDate(orgId, date);

        List<TeamMemberDailyDTO> memberEntries = entries.stream()
                .map(e -> TeamMemberDailyDTO.builder()
                        .userId(e.getUser().getId())
                        .userName(e.getUser().getUserName())
                        .firstName(e.getUser().getFirstName())
                        .lastName(e.getUser().getLastName())
                        .avatarUrl(e.getUser().getAvatarUrl())
                        .checkInTime(e.getCheckInTime())
                        .checkOutTime(e.getCheckOutTime())
                        .totalWorkMinutes(e.getTotalWorkMinutes())
                        .status(e.getStatus() != null ? e.getStatus().name() : null)
                        .workLocation(e.getWorkLocation() != null ? e.getWorkLocation().name() : null)
                        .isLateArrival(e.isLateArrival())
                        .build())
                .toList();

        return TeamDailyReportDTO.builder()
                .date(date)
                .entries(memberEntries)
                .build();
    }

    // ── Team Monthly Report ─────────────────────────────────────────────────────

    public TeamMonthlyReportDTO getTeamMonthlyReport(Long orgId, int year, int month) {
        YearMonth ym = YearMonth.of(year, month);
        LocalDate monthStart = ym.atDay(1);
        LocalDate monthEnd = ym.atEndOfMonth();

        List<TimeEntry> allEntries = timeEntryRepository.findByOrganizationIdAndWorkDateBetween(
                orgId, monthStart, monthEnd);

        // Group entries by user
        Map<Long, List<TimeEntry>> byUser = allEntries.stream()
                .collect(Collectors.groupingBy(e -> e.getUser().getId()));

        // Count day-off requests for the organization in this month
        List<DayOffRequest> dayOffRequests = dayOffRequestRepository
                .findByOrganizationIdAndStartDateBetweenAndStatus(orgId, monthStart, monthEnd, DayOffRequestStatus.APPROVED);
        Map<Long, Double> dayOffByUser = dayOffRequests.stream()
                .collect(Collectors.groupingBy(
                        r -> r.getUser().getId(),
                        Collectors.summingDouble(DayOffRequest::getTotalDays)));

        List<TeamMemberMonthlyDTO> members = new ArrayList<>();

        // Include all org users, even those with no entries
        List<User> orgUsers = userRepository.findByOrganizationId(orgId);
        for (User user : orgUsers) {
            List<TimeEntry> userEntries = byUser.getOrDefault(user.getId(), List.of());

            int totalWork = userEntries.stream().mapToInt(TimeEntry::getTotalWorkMinutes).sum();
            int daysWorked = userEntries.size();
            int overtime = userEntries.stream().mapToInt(TimeEntry::getOvertimeMinutes).sum();
            int lateArrivals = (int) userEntries.stream().filter(TimeEntry::isLateArrival).count();
            int earlyDepartures = (int) userEntries.stream().filter(TimeEntry::isEarlyDeparture).count();
            int daysOff = (int) Math.round(dayOffByUser.getOrDefault(user.getId(), 0.0));

            members.add(TeamMemberMonthlyDTO.builder()
                    .userId(user.getId())
                    .userName(user.getUserName())
                    .firstName(user.getFirstName())
                    .lastName(user.getLastName())
                    .totalWorkMinutes(totalWork)
                    .daysWorked(daysWorked)
                    .overtimeMinutes(overtime)
                    .lateArrivals(lateArrivals)
                    .earlyDepartures(earlyDepartures)
                    .daysOff(daysOff)
                    .build());
        }

        return TeamMonthlyReportDTO.builder()
                .year(year)
                .month(month)
                .members(members)
                .build();
    }

    // ── Admin Actions ─────────────────────────────────────────────────────────────

    @Transactional
    public void resetDailyEntry(Long userId, LocalDate date) {
        timeEntryRepository.findByUserIdAndWorkDate(userId, date)
                .ifPresent(timeEntryRepository::delete);
    }

    @Transactional
    public DailyReportDTO editTimeEntry(Long userId, LocalDate date, AdminTimeEditRequest request) {
        TimeEntry entry = timeEntryRepository.findByUserIdAndWorkDate(userId, date)
                .orElseThrow(() -> new IllegalStateException("No time entry found for this date"));

        // Update check-in/out times
        if (request.getCheckInTime() != null) {
            entry.setCheckInTime(request.getCheckInTime());
        }
        if (request.getCheckOutTime() != null) {
            entry.setCheckOutTime(request.getCheckOutTime());
            entry.setStatus(TimeEntryStatus.CHECKED_OUT);
        }

        // Update daily notes
        if (request.getDailyNotes() != null) {
            entry.setDailyNotes(request.getDailyNotes());
        }

        // Process break edits
        if (request.getBreaks() != null) {
            // Collect IDs of breaks to delete
            Set<Long> toDelete = request.getBreaks().stream()
                    .filter(b -> b.isDeleted() && b.getId() != null)
                    .map(AdminTimeEditRequest.BreakEditDTO::getId)
                    .collect(Collectors.toSet());

            // Remove deleted breaks
            entry.getBreaks().removeIf(b -> toDelete.contains(b.getId()));

            for (AdminTimeEditRequest.BreakEditDTO dto : request.getBreaks()) {
                if (dto.isDeleted()) continue;

                if (dto.getId() != null) {
                    // Update existing break
                    entry.getBreaks().stream()
                            .filter(b -> b.getId().equals(dto.getId()))
                            .findFirst()
                            .ifPresent(b -> {
                                b.setStartTime(dto.getStartTime());
                                b.setEndTime(dto.getEndTime());
                                b.setDurationMinutes(dto.getEndTime() != null
                                        ? (int) Duration.between(dto.getStartTime(), dto.getEndTime()).toMinutes()
                                        : 0);
                            });
                } else {
                    // Add new break
                    int duration = dto.getEndTime() != null
                            ? (int) Duration.between(dto.getStartTime(), dto.getEndTime()).toMinutes()
                            : 0;
                    BreakEntry newBreak = BreakEntry.builder()
                            .timeEntry(entry)
                            .startTime(dto.getStartTime())
                            .endTime(dto.getEndTime())
                            .durationMinutes(duration)
                            .build();
                    entry.getBreaks().add(newBreak);
                }
            }
        }

        // Recalculate totals
        recalculateEntry(entry);

        entry = timeEntryRepository.save(entry);
        return toDailyReportDTO(entry);
    }

    private void recalculateEntry(TimeEntry entry) {
        // Recalculate total break minutes
        int totalBreakMinutes = entry.getBreaks().stream()
                .mapToInt(BreakEntry::getDurationMinutes)
                .sum();
        entry.setTotalBreakMinutes(totalBreakMinutes);

        // Recalculate total work minutes
        if (entry.getCheckInTime() != null && entry.getCheckOutTime() != null) {
            long totalMinutes = Duration.between(entry.getCheckInTime(), entry.getCheckOutTime()).toMinutes();

            WorkTimeSettings orgSettings = getOrgSettingsForEntry(entry);
            boolean breakCountsAsWork = orgSettings != null && orgSettings.isBreakCountsAsWork();
            int workMinutes = (int) totalMinutes - (breakCountsAsWork ? 0 : totalBreakMinutes);
            if (workMinutes < 0) workMinutes = 0;
            entry.setTotalWorkMinutes(workMinutes);

            // Use per-user config for late/early/overtime
            UserWorkTimeConfig userConfig = userConfigService.getOrCreateConfig(entry.getUser());
            ZoneId zoneId = userConfig.getZoneId();

            // Recalculate late arrival / early departure
            if (orgSettings != null && orgSettings.isLateEarlyTrackingEnabled()) {
                LocalTime checkInLocal = entry.getCheckInTime().atZoneSameInstant(zoneId).toLocalTime();
                LocalTime checkOutLocal = entry.getCheckOutTime().atZoneSameInstant(zoneId).toLocalTime();
                entry.setLateArrival(checkInLocal.isAfter(userConfig.getWorkStartTime()));
                entry.setEarlyDeparture(checkOutLocal.isBefore(userConfig.getWorkEndTime()));
            }

            // Recalculate overtime
            entry.setOvertimeMinutes(0);
            if (orgSettings != null && orgSettings.isOvertimeTrackingEnabled()) {
                int dailyExpectedMinutes = (int) (userConfig.getDailyWorkingHours() * 60);
                if (workMinutes > dailyExpectedMinutes) {
                    entry.setOvertimeMinutes(workMinutes - dailyExpectedMinutes);
                }
            }
        }
    }

    private WorkTimeSettings getOrgSettingsForEntry(TimeEntry entry) {
        if (entry.getOrganization() == null) return null;
        return settingsRepository.findByOrganizationId(entry.getOrganization().getId()).orElse(null);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────────

    private DailyReportDTO toDailyReportDTO(TimeEntry e) {
        List<BreakEntryResponse> breakResponses = e.getBreaks() != null
                ? e.getBreaks().stream().map(BreakEntryResponse::from).toList()
                : List.of();

        return DailyReportDTO.builder()
                .date(e.getWorkDate())
                .checkInTime(e.getCheckInTime())
                .checkOutTime(e.getCheckOutTime())
                .totalWorkMinutes(e.getTotalWorkMinutes())
                .totalBreakMinutes(e.getTotalBreakMinutes())
                .overtimeMinutes(e.getOvertimeMinutes())
                .workLocation(e.getWorkLocation() != null ? e.getWorkLocation().name() : null)
                .isLateArrival(e.isLateArrival())
                .isEarlyDeparture(e.isEarlyDeparture())
                .dailyNotes(e.getDailyNotes())
                .breaks(breakResponses)
                .build();
    }

    private List<WeeklyReportDTO> buildWeeklyBreakdown(List<TimeEntry> entries, LocalDate monthStart, LocalDate monthEnd) {
        List<WeeklyReportDTO> weeks = new ArrayList<>();

        // Start from the Monday of the week containing monthStart
        LocalDate weekStart = monthStart.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));

        while (!weekStart.isAfter(monthEnd)) {
            LocalDate weekEnd = weekStart.plusDays(6);
            LocalDate effectiveStart = weekStart.isBefore(monthStart) ? monthStart : weekStart;
            LocalDate effectiveEnd = weekEnd.isAfter(monthEnd) ? monthEnd : weekEnd;

            List<TimeEntry> weekEntries = entries.stream()
                    .filter(e -> !e.getWorkDate().isBefore(effectiveStart) && !e.getWorkDate().isAfter(effectiveEnd))
                    .toList();

            if (!weekEntries.isEmpty()) {
                List<DailyReportDTO> dailyEntries = weekEntries.stream()
                        .map(this::toDailyReportDTO)
                        .toList();

                int totalWork = weekEntries.stream().mapToInt(TimeEntry::getTotalWorkMinutes).sum();
                int totalBreak = weekEntries.stream().mapToInt(TimeEntry::getTotalBreakMinutes).sum();
                int totalOvertime = weekEntries.stream().mapToInt(TimeEntry::getOvertimeMinutes).sum();
                int daysWorked = weekEntries.size();
                int avgWork = daysWorked > 0 ? totalWork / daysWorked : 0;

                weeks.add(WeeklyReportDTO.builder()
                        .weekStart(effectiveStart)
                        .weekEnd(effectiveEnd)
                        .totalWorkMinutes(totalWork)
                        .totalBreakMinutes(totalBreak)
                        .totalOvertimeMinutes(totalOvertime)
                        .daysWorked(daysWorked)
                        .dailyEntries(dailyEntries)
                        .averageWorkMinutesPerDay(avgWork)
                        .build());
            }

            weekStart = weekStart.plusWeeks(1);
        }

        return weeks;
    }
}
