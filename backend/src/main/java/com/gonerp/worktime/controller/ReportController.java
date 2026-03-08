package com.gonerp.worktime.controller;

import com.gonerp.common.ApiResponse;
import com.gonerp.usermanager.model.User;
import com.gonerp.usermanager.repository.UserRepository;
import com.gonerp.worktime.dto.*;
import com.gonerp.worktime.service.ExportService;
import com.gonerp.worktime.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/worktime/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;
    private final ExportService exportService;
    private final UserRepository userRepository;

    // ── My Reports ──────────────────────────────────────────────────────────────

    @GetMapping("/daily")
    public ResponseEntity<ApiResponse<DailyReportDTO>> getDailyReport(
            Authentication auth,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        User user = userRepository.findByUserName(auth.getName()).orElseThrow();
        return ResponseEntity.ok(ApiResponse.ok(reportService.getDailyReport(user.getId(), date)));
    }

    @GetMapping("/weekly")
    public ResponseEntity<ApiResponse<WeeklyReportDTO>> getWeeklyReport(
            Authentication auth,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekStart) {
        User user = userRepository.findByUserName(auth.getName()).orElseThrow();
        return ResponseEntity.ok(ApiResponse.ok(reportService.getWeeklyReport(user.getId(), weekStart)));
    }

    @GetMapping("/monthly")
    public ResponseEntity<ApiResponse<MonthlyReportDTO>> getMonthlyReport(
            Authentication auth,
            @RequestParam int year,
            @RequestParam int month) {
        User user = userRepository.findByUserName(auth.getName()).orElseThrow();
        return ResponseEntity.ok(ApiResponse.ok(reportService.getMonthlyReport(user.getId(), year, month)));
    }

    // ── Team Reports (Admin Only) ───────────────────────────────────────────────

    @GetMapping("/team/daily")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<TeamDailyReportDTO>> getTeamDailyReport(
            Authentication auth,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        User user = userRepository.findByUserName(auth.getName()).orElseThrow();
        Long orgId = getOrgIdOrThrow(user);
        return ResponseEntity.ok(ApiResponse.ok(reportService.getTeamDailyReport(orgId, date)));
    }

    @GetMapping("/team/monthly")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<TeamMonthlyReportDTO>> getTeamMonthlyReport(
            Authentication auth,
            @RequestParam int year,
            @RequestParam int month) {
        User user = userRepository.findByUserName(auth.getName()).orElseThrow();
        Long orgId = getOrgIdOrThrow(user);
        return ResponseEntity.ok(ApiResponse.ok(reportService.getTeamMonthlyReport(orgId, year, month)));
    }

    // ── Member Reports (Admin Only) ─────────────────────────────────────────────

    @GetMapping("/member/{userId}/daily")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<DailyReportDTO>> getMemberDailyReport(
            @PathVariable Long userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(ApiResponse.ok(reportService.getDailyReport(userId, date)));
    }

    @DeleteMapping("/member/{userId}/daily")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> resetMemberDailyEntry(
            @PathVariable Long userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        reportService.resetDailyEntry(userId, date);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @GetMapping("/member/{userId}/weekly")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<WeeklyReportDTO>> getMemberWeeklyReport(
            @PathVariable Long userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekStart) {
        return ResponseEntity.ok(ApiResponse.ok(reportService.getWeeklyReport(userId, weekStart)));
    }

    @GetMapping("/member/{userId}/monthly")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<MonthlyReportDTO>> getMemberMonthlyReport(
            @PathVariable Long userId,
            @RequestParam int year,
            @RequestParam int month) {
        return ResponseEntity.ok(ApiResponse.ok(reportService.getMonthlyReport(userId, year, month)));
    }

    @GetMapping("/export")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<byte[]> exportReport(
            Authentication auth,
            @RequestParam int year,
            @RequestParam int month,
            @RequestParam(defaultValue = "csv") String type) {
        User user = userRepository.findByUserName(auth.getName()).orElseThrow();
        Long orgId = getOrgIdOrThrow(user);

        byte[] data = exportService.exportTeamMonthlyCSV(orgId, year, month);

        String filename = String.format("team-report-%d-%02d.csv", year, month);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .contentLength(data.length)
                .body(data);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────────

    private Long getOrgIdOrThrow(User user) {
        if (user.getOrganization() == null) {
            throw new IllegalStateException("User does not belong to an organization");
        }
        return user.getOrganization().getId();
    }
}
