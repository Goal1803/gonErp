package com.gonerp.worktime.controller;

import com.gonerp.common.ApiResponse;
import com.gonerp.usermanager.model.User;
import com.gonerp.usermanager.repository.UserRepository;
import com.gonerp.worktime.dto.CalendarQueryResponse;
import com.gonerp.worktime.service.CalendarService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/worktime/calendar")
@RequiredArgsConstructor
public class CalendarController {

    private final CalendarService calendarService;
    private final UserRepository userRepository;

    @GetMapping
    public ResponseEntity<ApiResponse<CalendarQueryResponse>> getCalendarData(
            Authentication auth,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        User user = userRepository.findByUserName(auth.getName()).orElseThrow();
        Long orgId = user.getOrganization() != null ? user.getOrganization().getId() : null;
        if (orgId == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error("No organization"));
        }
        return ResponseEntity.ok(ApiResponse.ok(calendarService.getCalendarData(orgId, startDate, endDate)));
    }
}
