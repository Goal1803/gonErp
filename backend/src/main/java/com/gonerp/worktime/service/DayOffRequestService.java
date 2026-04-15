package com.gonerp.worktime.service;

import com.gonerp.usermanager.model.User;
import com.gonerp.usermanager.model.enums.RoleName;
import com.gonerp.usermanager.repository.UserRepository;
import com.gonerp.worktime.dto.DayOffRequestCreateDTO;
import com.gonerp.worktime.dto.DayOffRequestResponse;
import com.gonerp.worktime.event.WorkTimeNotificationEvent;
import com.gonerp.worktime.model.DayOffQuota;
import com.gonerp.worktime.model.DayOffRequest;
import com.gonerp.worktime.model.DayOffType;
import com.gonerp.worktime.model.PublicHoliday;
import com.gonerp.worktime.model.enums.DayOffRequestStatus;
import com.gonerp.worktime.model.enums.HalfDayType;
import com.gonerp.worktime.repository.DayOffQuotaRepository;
import com.gonerp.worktime.repository.DayOffRequestRepository;
import com.gonerp.worktime.repository.DayOffTypeRepository;
import com.gonerp.worktime.repository.PublicHolidayRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.MonthDay;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class DayOffRequestService {

    private final DayOffRequestRepository requestRepository;
    private final DayOffQuotaRepository quotaRepository;
    private final DayOffTypeRepository dayOffTypeRepository;
    private final PublicHolidayRepository publicHolidayRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional(readOnly = true)
    public List<DayOffRequestResponse> getMyRequests(Long userId) {
        return requestRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(DayOffRequestResponse::from)
                .toList();
    }

    public DayOffRequestResponse createRequest(Long userId, DayOffRequestCreateDTO dto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + userId));

        DayOffType dayOffType = dayOffTypeRepository.findById(dto.getDayOffTypeId())
                .orElseThrow(() -> new EntityNotFoundException("Day-off type not found: " + dto.getDayOffTypeId()));

        if (dto.getEndDate().isBefore(dto.getStartDate())) {
            throw new IllegalArgumentException("End date cannot be before start date");
        }

        boolean singleDay = dto.getStartDate().equals(dto.getEndDate());
        HalfDayType halfDayType = parseHalf(dto.getHalfDayType());
        HalfDayType startHalf = parseHalf(dto.getStartHalfDayType());
        HalfDayType endHalf = parseHalf(dto.getEndHalfDayType());

        // Compute total days (excludes weekends and public holidays)
        Long orgId = user.getOrganization() != null ? user.getOrganization().getId() : null;
        double totalDays = singleDay
                ? computeTotalDays(orgId, dto.getStartDate(), dto.getEndDate(), halfDayType)
                : computeRangeDays(orgId, dto.getStartDate(), dto.getEndDate(), startHalf, endHalf);

        // Check quota availability
        int year = dto.getStartDate().getYear();
        DayOffQuota quota = quotaRepository
                .findByUserIdAndDayOffTypeIdAndYear(userId, dto.getDayOffTypeId(), year)
                .orElse(null);

        if (quota != null) {
            double remaining = quota.getTotalDays() + quota.getCarriedOverDays() - quota.getUsedDays();
            if (totalDays > remaining) {
                throw new IllegalArgumentException(
                        "Insufficient quota. Remaining: " + remaining + " days, requested: " + totalDays + " days");
            }
        }

        DayOffRequest request = DayOffRequest.builder()
                .user(user)
                .organization(user.getOrganization())
                .dayOffType(dayOffType)
                .startDate(dto.getStartDate())
                .endDate(dto.getEndDate())
                .halfDayType(singleDay ? halfDayType : HalfDayType.FULL_DAY)
                .startHalfDayType(singleDay ? null : startHalf)
                .endHalfDayType(singleDay ? null : endHalf)
                .totalDays(totalDays)
                .reason(dto.getReason())
                .status(DayOffRequestStatus.PENDING)
                .build();

        DayOffRequest saved = requestRepository.save(request);

        // Notify all admins in the organization about the new request
        if (user.getOrganization() != null) {
            Set<Long> adminIds = userRepository.findByOrganizationId(user.getOrganization().getId()).stream()
                    .filter(u -> u.getRole() != null && (u.getRole().getName() == RoleName.ADMIN || u.getRole().getName() == RoleName.SUPER_ADMIN))
                    .map(User::getId)
                    .collect(Collectors.toSet());
            if (!adminIds.isEmpty()) {
                eventPublisher.publishEvent(new WorkTimeNotificationEvent(
                        userId,
                        adminIds,
                        "DAY_OFF_REQUESTED",
                        user.getFirstName() + " " + (user.getLastName() != null ? user.getLastName() : "")
                                + " requested " + request.getTotalDays() + " day(s) off ("
                                + dayOffType.getName() + ") from " + dto.getStartDate() + " to " + dto.getEndDate()
                ));
            }
        }

        return DayOffRequestResponse.from(saved);
    }

    public DayOffRequestResponse cancelRequest(Long id, Long userId) {
        DayOffRequest request = requestRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Day-off request not found: " + id));

        if (!request.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("You can only cancel your own requests");
        }

        if (request.getStatus() != DayOffRequestStatus.PENDING) {
            throw new IllegalStateException("Only pending requests can be cancelled");
        }

        request.setStatus(DayOffRequestStatus.CANCELLED);
        return DayOffRequestResponse.from(requestRepository.save(request));
    }

    @Transactional(readOnly = true)
    public List<DayOffRequestResponse> getPendingRequests(Long orgId) {
        return requestRepository.findByOrganizationIdAndStatus(orgId, DayOffRequestStatus.PENDING).stream()
                .map(DayOffRequestResponse::from)
                .toList();
    }

    public DayOffRequestResponse approve(Long id, Long reviewerId, String comment) {
        DayOffRequest request = requestRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Day-off request not found: " + id));

        if (request.getStatus() != DayOffRequestStatus.PENDING) {
            throw new IllegalStateException("Only pending requests can be approved");
        }

        User reviewer = userRepository.findById(reviewerId)
                .orElseThrow(() -> new EntityNotFoundException("Reviewer not found: " + reviewerId));

        request.setStatus(DayOffRequestStatus.APPROVED);
        request.setReviewedBy(reviewer);
        request.setReviewedAt(LocalDateTime.now());
        request.setReviewComment(comment);

        // Update quota used days
        int year = request.getStartDate().getYear();
        DayOffQuota quota = quotaRepository
                .findByUserIdAndDayOffTypeIdAndYear(request.getUser().getId(), request.getDayOffType().getId(), year)
                .orElse(null);

        if (quota != null) {
            quota.setUsedDays(quota.getUsedDays() + request.getTotalDays());
            quotaRepository.save(quota);
        }

        DayOffRequest savedApproved = requestRepository.save(request);

        // Notify the requester that their request was approved
        eventPublisher.publishEvent(new WorkTimeNotificationEvent(
                reviewerId,
                Set.of(request.getUser().getId()),
                "DAY_OFF_APPROVED",
                "Your day-off request (" + request.getDayOffType().getName() + ") from "
                        + request.getStartDate() + " to " + request.getEndDate() + " has been approved"
        ));

        return DayOffRequestResponse.from(savedApproved);
    }

    public List<DayOffRequestResponse> bulkApprove(List<Long> ids, Long reviewerId, String comment) {
        return ids.stream().map(id -> {
            try { return approve(id, reviewerId, comment); }
            catch (Exception e) { return null; }
        }).filter(x -> x != null).toList();
    }

    public List<DayOffRequestResponse> bulkDeny(List<Long> ids, Long reviewerId, String comment) {
        return ids.stream().map(id -> {
            try { return deny(id, reviewerId, comment); }
            catch (Exception e) { return null; }
        }).filter(x -> x != null).toList();
    }

    public DayOffRequestResponse adminRevoke(Long id, Long reviewerId, String comment) {
        DayOffRequest request = requestRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Day-off request not found: " + id));
        if (request.getStatus() != DayOffRequestStatus.APPROVED) {
            throw new IllegalStateException("Only approved requests can be revoked");
        }
        User reviewer = userRepository.findById(reviewerId)
                .orElseThrow(() -> new EntityNotFoundException("Reviewer not found: " + reviewerId));

        // Restore used days
        int year = request.getStartDate().getYear();
        quotaRepository.findByUserIdAndDayOffTypeIdAndYear(
                request.getUser().getId(), request.getDayOffType().getId(), year)
                .ifPresent(q -> {
                    q.setUsedDays(Math.max(0, q.getUsedDays() - request.getTotalDays()));
                    quotaRepository.save(q);
                });

        request.setStatus(DayOffRequestStatus.CANCELLED);
        request.setReviewedBy(reviewer);
        request.setReviewedAt(LocalDateTime.now());
        String prev = request.getReviewComment();
        request.setReviewComment("[Revoked] " + (comment != null ? comment : "") + (prev != null ? " | prev: " + prev : ""));

        DayOffRequest saved = requestRepository.save(request);
        eventPublisher.publishEvent(new WorkTimeNotificationEvent(
                reviewerId,
                Set.of(request.getUser().getId()),
                "DAY_OFF_DENIED",
                "Your approved day-off (" + request.getDayOffType().getName() + ") from "
                        + request.getStartDate() + " to " + request.getEndDate() + " was revoked by admin"
                        + (comment != null && !comment.isBlank() ? ": " + comment : "")
        ));
        return DayOffRequestResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public List<java.util.Map<String, Object>> overlappingPeers(Long userId, LocalDate start, LocalDate end) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + userId));
        if (user.getOrganization() == null) return List.of();
        List<DayOffRequest> rows = requestRepository.findOverlappingRequests(
                user.getOrganization().getId(),
                List.of(DayOffRequestStatus.PENDING, DayOffRequestStatus.APPROVED),
                start, end);
        return rows.stream()
                .filter(r -> r.getUser() != null && !r.getUser().getId().equals(userId))
                .map(r -> {
                    java.util.Map<String, Object> m = new java.util.HashMap<>();
                    m.put("userId", r.getUser().getId());
                    m.put("userName", r.getUser().getUserName());
                    m.put("firstName", r.getUser().getFirstName());
                    m.put("lastName", r.getUser().getLastName());
                    m.put("status", r.getStatus().name());
                    m.put("startDate", r.getStartDate());
                    m.put("endDate", r.getEndDate());
                    m.put("dayOffTypeName", r.getDayOffType() != null ? r.getDayOffType().getName() : null);
                    m.put("dayOffTypeColor", r.getDayOffType() != null ? r.getDayOffType().getColor() : null);
                    return m;
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public java.util.Map<String, Object> reportSummary(Long orgId, LocalDate from, LocalDate to) {
        List<DayOffRequest> approved = requestRepository.searchByOrgFilters(
                orgId, DayOffRequestStatus.APPROVED, null, null, from, to);
        List<DayOffRequest> pending = requestRepository.searchByOrgFilters(
                orgId, DayOffRequestStatus.PENDING, null, null, from, to);
        List<DayOffRequest> denied = requestRepository.searchByOrgFilters(
                orgId, DayOffRequestStatus.DENIED, null, null, from, to);

        double totalApprovedDays = approved.stream().mapToDouble(DayOffRequest::getTotalDays).sum();

        // days per type (approved only)
        java.util.Map<String, Double> daysByType = new java.util.LinkedHashMap<>();
        java.util.Map<String, String> colorByType = new java.util.HashMap<>();
        for (DayOffRequest r : approved) {
            String name = r.getDayOffType() != null ? r.getDayOffType().getName() : "Unknown";
            daysByType.merge(name, r.getTotalDays(), Double::sum);
            if (r.getDayOffType() != null && r.getDayOffType().getColor() != null)
                colorByType.putIfAbsent(name, r.getDayOffType().getColor());
        }

        // top users by approved days
        java.util.Map<Long, Double> daysByUser = new java.util.HashMap<>();
        java.util.Map<Long, String> userNames = new java.util.HashMap<>();
        for (DayOffRequest r : approved) {
            if (r.getUser() == null) continue;
            Long uid = r.getUser().getId();
            daysByUser.merge(uid, r.getTotalDays(), Double::sum);
            userNames.putIfAbsent(uid, r.getUser().getUserName());
        }
        List<java.util.Map<String, Object>> topUsers = daysByUser.entrySet().stream()
                .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                .limit(10)
                .map(e -> {
                    java.util.Map<String, Object> m = new java.util.HashMap<>();
                    m.put("userId", e.getKey());
                    m.put("userName", userNames.get(e.getKey()));
                    m.put("days", e.getValue());
                    return m;
                })
                .toList();

        java.util.Map<String, Object> out = new java.util.HashMap<>();
        out.put("approvedCount", approved.size());
        out.put("pendingCount", pending.size());
        out.put("deniedCount", denied.size());
        out.put("totalApprovedDays", totalApprovedDays);
        out.put("daysByType", daysByType.entrySet().stream().map(e -> {
            java.util.Map<String, Object> m = new java.util.HashMap<>();
            m.put("name", e.getKey());
            m.put("days", e.getValue());
            m.put("color", colorByType.get(e.getKey()));
            return m;
        }).toList());
        out.put("topUsers", topUsers);
        return out;
    }

    @Transactional(readOnly = true)
    public List<DayOffRequestResponse> adminList(Long orgId, String status, Long userId, Long typeId,
                                                  LocalDate from, LocalDate to) {
        DayOffRequestStatus st = null;
        if (status != null && !status.isBlank() && !"ALL".equalsIgnoreCase(status)) {
            try { st = DayOffRequestStatus.valueOf(status.toUpperCase()); } catch (IllegalArgumentException ignored) { }
        }
        return requestRepository.searchByOrgFilters(orgId, st, userId, typeId, from, to).stream()
                .map(DayOffRequestResponse::from)
                .toList();
    }

    public DayOffRequestResponse deny(Long id, Long reviewerId, String comment) {
        DayOffRequest request = requestRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Day-off request not found: " + id));

        if (request.getStatus() != DayOffRequestStatus.PENDING) {
            throw new IllegalStateException("Only pending requests can be denied");
        }

        User reviewer = userRepository.findById(reviewerId)
                .orElseThrow(() -> new EntityNotFoundException("Reviewer not found: " + reviewerId));

        request.setStatus(DayOffRequestStatus.DENIED);
        request.setReviewedBy(reviewer);
        request.setReviewedAt(LocalDateTime.now());
        request.setReviewComment(comment);

        DayOffRequest savedDenied = requestRepository.save(request);

        // Notify the requester that their request was denied
        eventPublisher.publishEvent(new WorkTimeNotificationEvent(
                reviewerId,
                Set.of(request.getUser().getId()),
                "DAY_OFF_DENIED",
                "Your day-off request (" + request.getDayOffType().getName() + ") from "
                        + request.getStartDate() + " to " + request.getEndDate() + " has been denied"
                        + (comment != null && !comment.isBlank() ? ": " + comment : "")
        ));

        return DayOffRequestResponse.from(savedDenied);
    }

    // ── Helpers ─────────────────────────────────────────────────────────────────

    private HalfDayType parseHalf(String raw) {
        if (raw == null || raw.isBlank()) return HalfDayType.FULL_DAY;
        try { return HalfDayType.valueOf(raw.toUpperCase()); }
        catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid half-day type: " + raw);
        }
    }

    /** Multi-day range: count weekdays/non-holidays; subtract 0.5 on start/end if marked half. */
    private double computeRangeDays(Long orgId, LocalDate start, LocalDate end, HalfDayType startHalf, HalfDayType endHalf) {
        Set<LocalDate> holidays = resolveHolidayDates(orgId, start, end);
        double days = 0;
        LocalDate current = start;
        while (!current.isAfter(end)) {
            DayOfWeek dow = current.getDayOfWeek();
            boolean counted = dow != DayOfWeek.SATURDAY && dow != DayOfWeek.SUNDAY && !holidays.contains(current);
            if (counted) days += 1.0;
            current = current.plusDays(1);
        }
        DayOfWeek startDow = start.getDayOfWeek();
        boolean startCounted = startDow != DayOfWeek.SATURDAY && startDow != DayOfWeek.SUNDAY && !holidays.contains(start);
        if (startCounted && (startHalf == HalfDayType.MORNING || startHalf == HalfDayType.AFTERNOON)) days -= 0.5;

        DayOfWeek endDow = end.getDayOfWeek();
        boolean endCounted = endDow != DayOfWeek.SATURDAY && endDow != DayOfWeek.SUNDAY && !holidays.contains(end);
        if (endCounted && (endHalf == HalfDayType.MORNING || endHalf == HalfDayType.AFTERNOON)) days -= 0.5;

        return Math.max(0, days);
    }

    private double computeTotalDays(Long orgId, LocalDate startDate, LocalDate endDate, HalfDayType halfDayType) {
        Set<LocalDate> holidays = resolveHolidayDates(orgId, startDate, endDate);

        if (halfDayType == HalfDayType.MORNING || halfDayType == HalfDayType.AFTERNOON) {
            // Half day only meaningful for single-day requests; if it falls on a weekend/holiday → 0
            DayOfWeek dow = startDate.getDayOfWeek();
            if (dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY || holidays.contains(startDate)) return 0;
            return 0.5;
        }

        double days = 0;
        LocalDate current = startDate;
        while (!current.isAfter(endDate)) {
            DayOfWeek dow = current.getDayOfWeek();
            if (dow != DayOfWeek.SATURDAY && dow != DayOfWeek.SUNDAY && !holidays.contains(current)) {
                days += 1.0;
            }
            current = current.plusDays(1);
        }
        return days;
    }

    private Set<LocalDate> resolveHolidayDates(Long orgId, LocalDate start, LocalDate end) {
        Set<LocalDate> dates = new HashSet<>();
        if (orgId == null) return dates;

        // Non-recurring holidays whose [start..end] overlaps the query window.
        // Use a broad fetch (all holidays for org) and filter — schema allows multi-day via endDate.
        List<PublicHoliday> all = publicHolidayRepository.findByOrganizationId(orgId);
        for (PublicHoliday h : all) {
            if (h.isRecurring()) continue;
            LocalDate hStart = h.getHolidayDate();
            LocalDate hEnd = h.getEndDate() != null ? h.getEndDate() : hStart;
            addRangeIfOverlaps(dates, hStart, hEnd, start, end);
        }

        // Recurring holidays: map into each year covered by the query range.
        for (PublicHoliday h : all) {
            if (!h.isRecurring()) continue;
            LocalDate hStart = h.getHolidayDate();
            LocalDate hEnd = h.getEndDate() != null ? h.getEndDate() : hStart;
            MonthDay mdStart = MonthDay.from(hStart);
            int spanDays = (int) java.time.temporal.ChronoUnit.DAYS.between(hStart, hEnd);
            for (int year = start.getYear(); year <= end.getYear(); year++) {
                LocalDate candidateStart;
                try { candidateStart = mdStart.atYear(year); } catch (Exception e) { continue; }
                LocalDate candidateEnd = candidateStart.plusDays(spanDays);
                addRangeIfOverlaps(dates, candidateStart, candidateEnd, start, end);
            }
        }
        return dates;
    }

    private void addRangeIfOverlaps(Set<LocalDate> out, LocalDate hStart, LocalDate hEnd,
                                    LocalDate qStart, LocalDate qEnd) {
        LocalDate from = hStart.isBefore(qStart) ? qStart : hStart;
        LocalDate to = hEnd.isAfter(qEnd) ? qEnd : hEnd;
        if (from.isAfter(to)) return;
        LocalDate cur = from;
        while (!cur.isAfter(to)) { out.add(cur); cur = cur.plusDays(1); }
    }

    /** Public helper so callers (dialog preview) can compute days without creating a request. */
    @Transactional(readOnly = true)
    public double previewTotalDays(Long userId, LocalDate startDate, LocalDate endDate,
                                   String halfDay, String startHalfDay, String endHalfDay) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + userId));
        Long orgId = user.getOrganization() != null ? user.getOrganization().getId() : null;
        if (startDate.equals(endDate)) {
            HalfDayType hd = HalfDayType.FULL_DAY;
            if (halfDay != null && !halfDay.isBlank()) {
                try { hd = HalfDayType.valueOf(halfDay.toUpperCase()); } catch (IllegalArgumentException ignored) { }
            }
            return computeTotalDays(orgId, startDate, endDate, hd);
        }
        HalfDayType startH = HalfDayType.FULL_DAY, endH = HalfDayType.FULL_DAY;
        if (startHalfDay != null && !startHalfDay.isBlank()) {
            try { startH = HalfDayType.valueOf(startHalfDay.toUpperCase()); } catch (IllegalArgumentException ignored) { }
        }
        if (endHalfDay != null && !endHalfDay.isBlank()) {
            try { endH = HalfDayType.valueOf(endHalfDay.toUpperCase()); } catch (IllegalArgumentException ignored) { }
        }
        return computeRangeDays(orgId, startDate, endDate, startH, endH);
    }
}
