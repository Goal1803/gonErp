package com.gonerp.worktime.controller;

import com.gonerp.common.ApiResponse;
import com.gonerp.worktime.dto.*;
import com.gonerp.worktime.service.TimeClockService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/worktime/clock")
@RequiredArgsConstructor
public class TimeClockController {

    private final TimeClockService timeClockService;

    @PostMapping("/check-in")
    public ResponseEntity<ApiResponse<TimeEntryResponse>> checkIn(@RequestBody(required = false) CheckInRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Checked in", timeClockService.checkIn(request)));
    }

    @PostMapping("/pause")
    public ResponseEntity<ApiResponse<TimeEntryResponse>> pause() {
        return ResponseEntity.ok(ApiResponse.ok("Break started", timeClockService.pause()));
    }

    @PostMapping("/resume")
    public ResponseEntity<ApiResponse<TimeEntryResponse>> resume() {
        return ResponseEntity.ok(ApiResponse.ok("Break ended", timeClockService.resume()));
    }

    @PostMapping("/check-out")
    public ResponseEntity<ApiResponse<TimeEntryResponse>> checkOut(@RequestBody(required = false) CheckOutRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Checked out", timeClockService.checkOut(request)));
    }

    @GetMapping("/status")
    public ResponseEntity<ApiResponse<ClockStatusResponse>> getStatus() {
        return ResponseEntity.ok(ApiResponse.ok(timeClockService.getStatus()));
    }

    @GetMapping("/today")
    public ResponseEntity<ApiResponse<TimeEntryResponse>> getToday() {
        return ResponseEntity.ok(ApiResponse.ok(timeClockService.getToday()));
    }
}
