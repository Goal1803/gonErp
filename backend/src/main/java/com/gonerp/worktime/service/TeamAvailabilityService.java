package com.gonerp.worktime.service;

import com.gonerp.usermanager.model.User;
import com.gonerp.usermanager.repository.UserRepository;
import com.gonerp.worktime.dto.MemberAvailabilityDTO;
import com.gonerp.worktime.dto.TeamAvailabilityDTO;
import com.gonerp.worktime.model.DayOffRequest;
import com.gonerp.worktime.model.TimeEntry;
import com.gonerp.worktime.model.enums.DayOffRequestStatus;
import com.gonerp.worktime.model.enums.TimeEntryStatus;
import com.gonerp.worktime.repository.DayOffRequestRepository;
import com.gonerp.worktime.repository.TimeEntryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TeamAvailabilityService {

    private final TimeEntryRepository timeEntryRepository;
    private final DayOffRequestRepository dayOffRequestRepository;
    private final UserRepository userRepository;

    public TeamAvailabilityDTO getTeamAvailability(Long orgId) {
        LocalDate today = LocalDate.now();

        List<User> orgUsers = userRepository.findByOrganizationId(orgId);
        List<TimeEntry> todayEntries = timeEntryRepository.findByOrganizationIdAndWorkDate(orgId, today);

        // Map entries by userId for quick lookup
        Map<Long, TimeEntry> entryByUser = todayEntries.stream()
                .collect(Collectors.toMap(e -> e.getUser().getId(), e -> e, (a, b) -> a));

        // Find approved day-off requests covering today
        List<DayOffRequest> dayOffRequests = dayOffRequestRepository
                .findByOrganizationIdAndStartDateBetweenAndStatus(orgId, today, today, DayOffRequestStatus.APPROVED);

        // Also check requests where today falls between startDate and endDate
        // The repository query checks startDate between range, but we also need to check
        // requests that started before today but haven't ended
        Set<Long> usersOnDayOff = dayOffRequests.stream()
                .filter(r -> !today.isBefore(r.getStartDate()) && !today.isAfter(r.getEndDate()))
                .map(r -> r.getUser().getId())
                .collect(Collectors.toSet());

        List<MemberAvailabilityDTO> members = new ArrayList<>();

        for (User user : orgUsers) {
            TimeEntry entry = entryByUser.get(user.getId());
            String status;
            java.time.LocalDateTime checkInTime = null;
            String workLocation = null;

            if (usersOnDayOff.contains(user.getId())) {
                status = "DAY_OFF";
            } else if (entry != null) {
                if (entry.getStatus() == TimeEntryStatus.CHECKED_IN) {
                    status = "WORKING";
                } else if (entry.getStatus() == TimeEntryStatus.ON_BREAK) {
                    status = "ON_BREAK";
                } else {
                    status = "OFF"; // CHECKED_OUT
                }
                checkInTime = entry.getCheckInTime();
                workLocation = entry.getWorkLocation() != null ? entry.getWorkLocation().name() : null;
            } else {
                status = "NOT_CHECKED_IN";
            }

            members.add(MemberAvailabilityDTO.builder()
                    .userId(user.getId())
                    .userName(user.getUserName())
                    .firstName(user.getFirstName())
                    .lastName(user.getLastName())
                    .avatarUrl(user.getAvatarUrl())
                    .status(status)
                    .checkInTime(checkInTime)
                    .workLocation(workLocation)
                    .build());
        }

        return TeamAvailabilityDTO.builder()
                .date(today)
                .members(members)
                .build();
    }
}
