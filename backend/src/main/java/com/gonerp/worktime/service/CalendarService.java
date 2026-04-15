package com.gonerp.worktime.service;

import com.gonerp.usermanager.model.User;
import com.gonerp.usermanager.model.enums.UserStatus;
import com.gonerp.usermanager.repository.UserRepository;
import com.gonerp.worktime.dto.*;
import com.gonerp.worktime.model.DayOffRequest;
import com.gonerp.worktime.model.PublicHoliday;
import com.gonerp.worktime.model.enums.DayOffRequestStatus;
import com.gonerp.worktime.repository.DayOffRequestRepository;
import com.gonerp.worktime.repository.PublicHolidayRepository;
import java.util.Comparator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CalendarService {

    private final UserRepository userRepository;
    private final DayOffRequestRepository dayOffRequestRepository;
    private final PublicHolidayRepository publicHolidayRepository;

    public CalendarQueryResponse getCalendarData(Long orgId, LocalDate startDate, LocalDate endDate) {
        // 1. Get all active users in the organization, sorted by first name then user name
        List<User> users = userRepository.findByOrganizationId(orgId).stream()
                .filter(u -> u.getStatus() == UserStatus.ACTIVE)
                .sorted(Comparator
                        .comparing((User u) -> u.getFirstName() != null ? u.getFirstName() : u.getUserName(),
                                   Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
                        .thenComparing(User::getUserName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                .toList();
        List<CalendarUserDTO> userDTOs = users.stream()
                .map(CalendarUserDTO::from)
                .toList();

        // 2. Get all approved + pending day-off requests that overlap the date range
        List<DayOffRequestStatus> statuses = List.of(
                DayOffRequestStatus.APPROVED,
                DayOffRequestStatus.PENDING
        );
        List<DayOffRequest> requests = dayOffRequestRepository.findOverlappingRequests(
                orgId, statuses, startDate, endDate);

        // 3. Get holidays (fixed + recurring), expand multi-day ranges, and collect
        //    per-day metadata. Output a per-occurrence PublicHolidayResponse for the client.
        List<PublicHoliday> allHolidays = publicHolidayRepository.findByOrganizationId(orgId);

        Set<LocalDate> holidayDateSet = new HashSet<>();
        Map<LocalDate, String> holidayNameMap = new HashMap<>();
        Map<LocalDate, String> holidayColorMap = new HashMap<>();
        List<PublicHolidayResponse> holidayResponses = new ArrayList<>();

        for (PublicHoliday h : allHolidays) {
            LocalDate hStart = h.getHolidayDate();
            LocalDate hEnd = h.getEndDate() != null ? h.getEndDate() : hStart;

            if (!h.isRecurring()) {
                addHolidayOccurrence(holidayDateSet, holidayNameMap, holidayColorMap,
                        holidayResponses, orgId, h, hStart, hEnd, startDate, endDate);
            } else {
                int spanDays = (int) java.time.temporal.ChronoUnit.DAYS.between(hStart, hEnd);
                java.time.MonthDay md = java.time.MonthDay.from(hStart);
                for (int year = startDate.getYear(); year <= endDate.getYear(); year++) {
                    LocalDate candStart;
                    try { candStart = md.atYear(year); } catch (Exception e) { continue; }
                    LocalDate candEnd = candStart.plusDays(spanDays);
                    addHolidayOccurrence(holidayDateSet, holidayNameMap, holidayColorMap,
                            holidayResponses, orgId, h, candStart, candEnd, startDate, endDate);
                }
            }
        }

        // 4. Build CalendarDayDTO list for each user-date combination that has a request or holiday
        List<CalendarDayDTO> days = new ArrayList<>();

        for (DayOffRequest request : requests) {
            User reqUser = request.getUser();
            LocalDate fullStart = request.getStartDate();
            LocalDate fullEnd = request.getEndDate();
            boolean singleDay = fullStart.equals(fullEnd);
            LocalDate reqStart = fullStart.isBefore(startDate) ? startDate : fullStart;
            LocalDate reqEnd = fullEnd.isAfter(endDate) ? endDate : fullEnd;

            LocalDate current = reqStart;
            while (!current.isAfter(reqEnd)) {
                String perDayHalf = "FULL_DAY";
                if (singleDay) {
                    perDayHalf = request.getHalfDayType() != null ? request.getHalfDayType().name() : "FULL_DAY";
                } else if (current.equals(fullStart) && request.getStartHalfDayType() != null) {
                    perDayHalf = request.getStartHalfDayType().name();
                } else if (current.equals(fullEnd) && request.getEndHalfDayType() != null) {
                    perDayHalf = request.getEndHalfDayType().name();
                }
                CalendarDayDTO day = CalendarDayDTO.builder()
                        .date(current)
                        .userId(reqUser.getId())
                        .userName(reqUser.getUserName())
                        .userFirstName(reqUser.getFirstName())
                        .userLastName(reqUser.getLastName())
                        .userAvatarUrl(reqUser.getAvatarUrl())
                        .dayOffTypeId(request.getDayOffType() != null ? request.getDayOffType().getId() : null)
                        .dayOffTypeName(request.getDayOffType() != null ? request.getDayOffType().getName() : null)
                        .dayOffTypeColor(request.getDayOffType() != null ? request.getDayOffType().getColor() : null)
                        .requestId(request.getId())
                        .status(request.getStatus() != null ? request.getStatus().name() : null)
                        .halfDayType(perDayHalf)
                        .isHoliday(holidayDateSet.contains(current))
                        .holidayName(holidayNameMap.getOrDefault(current, null))
                        .build();
                days.add(day);
                current = current.plusDays(1);
            }
        }

        return CalendarQueryResponse.builder()
                .users(userDTOs)
                .days(days)
                .holidays(holidayResponses)
                .startDate(startDate)
                .endDate(endDate)
                .build();
    }

    /**
     * Populate day/name/color maps and append a per-occurrence PublicHolidayResponse
     * if the expanded occurrence [occStart..occEnd] overlaps the query window.
     */
    private void addHolidayOccurrence(Set<LocalDate> dateSet,
                                      Map<LocalDate, String> nameMap,
                                      Map<LocalDate, String> colorMap,
                                      List<PublicHolidayResponse> out,
                                      Long orgId,
                                      PublicHoliday template,
                                      LocalDate occStart,
                                      LocalDate occEnd,
                                      LocalDate qStart,
                                      LocalDate qEnd) {
        LocalDate from = occStart.isBefore(qStart) ? qStart : occStart;
        LocalDate to = occEnd.isAfter(qEnd) ? qEnd : occEnd;
        if (from.isAfter(to)) return;
        LocalDate cur = from;
        while (!cur.isAfter(to)) {
            dateSet.add(cur);
            nameMap.putIfAbsent(cur, template.getName());
            if (template.getColor() != null) colorMap.putIfAbsent(cur, template.getColor());
            cur = cur.plusDays(1);
        }
        out.add(PublicHolidayResponse.builder()
                .id(template.getId())
                .holidayDate(occStart)
                .endDate(occEnd.equals(occStart) ? null : occEnd)
                .name(template.getName())
                .color(template.getColor())
                .isRecurring(template.isRecurring())
                .organizationId(orgId)
                .build());
    }
}
