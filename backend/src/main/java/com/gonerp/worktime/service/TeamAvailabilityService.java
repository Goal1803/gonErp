package com.gonerp.worktime.service;

import com.gonerp.usermanager.model.User;
import com.gonerp.usermanager.repository.UserRepository;
import com.gonerp.worktime.dto.MemberAvailabilityDTO;
import com.gonerp.worktime.dto.TeamAvailabilityDTO;
import com.gonerp.worktime.model.DayOffRequest;
import com.gonerp.worktime.model.TimeEntry;
import com.gonerp.worktime.model.UserWorkTimeConfig;
import com.gonerp.worktime.model.enums.DayOffRequestStatus;
import com.gonerp.worktime.model.enums.TimeEntryStatus;
import com.gonerp.worktime.repository.DayOffRequestRepository;
import com.gonerp.worktime.repository.TimeEntryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
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
    private final UserWorkTimeConfigService userConfigService;

    @Transactional
    public TeamAvailabilityDTO getTeamAvailability(Long orgId) {
        List<User> orgUsers = userRepository.findByOrganizationId(orgId);

        // Collect all possible "today" dates across user timezones to query entries
        // Use a broad approach: query entries for today in each user's timezone
        List<MemberAvailabilityDTO> members = new ArrayList<>();

        for (User user : orgUsers) {
            UserWorkTimeConfig userConfig = userConfigService.getOrCreateConfig(user);
            ZoneId userZoneId = userConfig.getZoneId();
            LocalDate userToday = LocalDate.now(userZoneId);

            // Find this user's entry for their "today"
            TimeEntry entry = timeEntryRepository.findByUserIdAndWorkDate(user.getId(), userToday).orElse(null);

            // Check day-off for user's today
            boolean onDayOff = dayOffRequestRepository
                    .findByUserIdAndStartDateBetween(user.getId(), userToday, userToday)
                    .stream()
                    .filter(r -> r.getStatus() == DayOffRequestStatus.APPROVED)
                    .anyMatch(r -> !userToday.isBefore(r.getStartDate()) && !userToday.isAfter(r.getEndDate()));

            String status;
            OffsetDateTime checkInTime = null;
            String workLocation = null;

            if (onDayOff) {
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

        // Use system date for the response (admin's perspective)
        return TeamAvailabilityDTO.builder()
                .date(LocalDate.now())
                .members(members)
                .build();
    }
}
