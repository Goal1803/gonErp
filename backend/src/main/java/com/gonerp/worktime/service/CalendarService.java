package com.gonerp.worktime.service;

import com.gonerp.usermanager.model.User;
import com.gonerp.usermanager.repository.UserRepository;
import com.gonerp.worktime.dto.*;
import com.gonerp.worktime.model.DayOffRequest;
import com.gonerp.worktime.model.PublicHoliday;
import com.gonerp.worktime.model.enums.DayOffRequestStatus;
import com.gonerp.worktime.repository.DayOffRequestRepository;
import com.gonerp.worktime.repository.PublicHolidayRepository;
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
        // 1. Get all users in the organization
        List<User> users = userRepository.findByOrganizationId(orgId);
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

        // 3. Get holidays in the date range
        List<PublicHoliday> holidays = publicHolidayRepository
                .findByOrganizationIdAndHolidayDateBetween(orgId, startDate, endDate);

        // Also include recurring holidays mapped to the current year range
        List<PublicHoliday> recurringHolidays = publicHolidayRepository
                .findByOrganizationIdAndIsRecurring(orgId, true);

        // Build a set of holiday dates for quick lookup
        Set<LocalDate> holidayDateSet = new HashSet<>();
        Map<LocalDate, String> holidayNameMap = new HashMap<>();

        for (PublicHoliday h : holidays) {
            holidayDateSet.add(h.getHolidayDate());
            holidayNameMap.put(h.getHolidayDate(), h.getName());
        }

        // Map recurring holidays into the query range
        for (PublicHoliday rh : recurringHolidays) {
            // For each year in the range, create the recurring date
            for (int year = startDate.getYear(); year <= endDate.getYear(); year++) {
                try {
                    LocalDate recurringDate = rh.getHolidayDate().withYear(year);
                    if (!recurringDate.isBefore(startDate) && !recurringDate.isAfter(endDate)) {
                        if (!holidayDateSet.contains(recurringDate)) {
                            holidayDateSet.add(recurringDate);
                            holidayNameMap.put(recurringDate, rh.getName());
                        }
                    }
                } catch (Exception ignored) {
                    // e.g., Feb 29 in a non-leap year
                }
            }
        }

        // 4. Build CalendarDayDTO list for each user-date combination that has a request or holiday
        List<CalendarDayDTO> days = new ArrayList<>();

        for (DayOffRequest request : requests) {
            User reqUser = request.getUser();
            LocalDate reqStart = request.getStartDate().isBefore(startDate) ? startDate : request.getStartDate();
            LocalDate reqEnd = request.getEndDate().isAfter(endDate) ? endDate : request.getEndDate();

            LocalDate current = reqStart;
            while (!current.isAfter(reqEnd)) {
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
                        .halfDayType(request.getHalfDayType() != null ? request.getHalfDayType().name() : null)
                        .isHoliday(holidayDateSet.contains(current))
                        .holidayName(holidayNameMap.getOrDefault(current, null))
                        .build();
                days.add(day);
                current = current.plusDays(1);
            }
        }

        // Build holiday response list (including recurring mapped ones)
        List<PublicHolidayResponse> holidayResponses = new ArrayList<>();
        for (PublicHoliday h : holidays) {
            holidayResponses.add(PublicHolidayResponse.from(h));
        }
        // Add recurring holiday entries that were mapped to the range
        for (PublicHoliday rh : recurringHolidays) {
            for (int year = startDate.getYear(); year <= endDate.getYear(); year++) {
                try {
                    LocalDate recurringDate = rh.getHolidayDate().withYear(year);
                    if (!recurringDate.isBefore(startDate) && !recurringDate.isAfter(endDate)) {
                        boolean alreadyExists = holidays.stream()
                                .anyMatch(h -> h.getHolidayDate().equals(recurringDate));
                        if (!alreadyExists) {
                            holidayResponses.add(PublicHolidayResponse.builder()
                                    .id(rh.getId())
                                    .holidayDate(recurringDate)
                                    .name(rh.getName())
                                    .isRecurring(true)
                                    .organizationId(orgId)
                                    .build());
                        }
                    }
                } catch (Exception ignored) {
                    // e.g., Feb 29 in a non-leap year
                }
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
}
