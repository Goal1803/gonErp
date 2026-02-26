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
import com.gonerp.worktime.model.enums.DayOffRequestStatus;
import com.gonerp.worktime.model.enums.HalfDayType;
import com.gonerp.worktime.repository.DayOffQuotaRepository;
import com.gonerp.worktime.repository.DayOffRequestRepository;
import com.gonerp.worktime.repository.DayOffTypeRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
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

        // Parse half-day type
        HalfDayType halfDayType = HalfDayType.FULL_DAY;
        if (dto.getHalfDayType() != null && !dto.getHalfDayType().isBlank()) {
            try {
                halfDayType = HalfDayType.valueOf(dto.getHalfDayType().toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid half-day type: " + dto.getHalfDayType());
            }
        }

        // Compute total days
        double totalDays = computeTotalDays(dto.getStartDate(), dto.getEndDate(), halfDayType);

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
                .halfDayType(halfDayType)
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

    private double computeTotalDays(LocalDate startDate, LocalDate endDate, HalfDayType halfDayType) {
        if (halfDayType == HalfDayType.MORNING || halfDayType == HalfDayType.AFTERNOON) {
            return 0.5;
        }

        // Count weekdays between start and end (inclusive)
        double days = 0;
        LocalDate current = startDate;
        while (!current.isAfter(endDate)) {
            DayOfWeek dow = current.getDayOfWeek();
            if (dow != DayOfWeek.SATURDAY && dow != DayOfWeek.SUNDAY) {
                days += 1.0;
            }
            current = current.plusDays(1);
        }
        return days;
    }
}
