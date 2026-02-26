package com.gonerp.worktime.controller;

import com.gonerp.common.ApiResponse;
import com.gonerp.usermanager.model.User;
import com.gonerp.usermanager.repository.UserRepository;
import com.gonerp.worktime.dto.DayOffTypeRequest;
import com.gonerp.worktime.dto.DayOffTypeResponse;
import com.gonerp.worktime.service.DayOffTypeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/worktime/day-off-types")
@RequiredArgsConstructor
public class DayOffTypeController {

    private final DayOffTypeService dayOffTypeService;
    private final UserRepository userRepository;

    @GetMapping
    public ResponseEntity<ApiResponse<List<DayOffTypeResponse>>> getAll(Authentication auth) {
        User user = userRepository.findByUserName(auth.getName()).orElseThrow();
        Long orgId = user.getOrganization() != null ? user.getOrganization().getId() : null;
        if (orgId == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error("No organization"));
        }
        return ResponseEntity.ok(ApiResponse.ok(dayOffTypeService.getAllForOrg(orgId)));
    }

    @GetMapping("/active")
    public ResponseEntity<ApiResponse<List<DayOffTypeResponse>>> getActive(Authentication auth) {
        User user = userRepository.findByUserName(auth.getName()).orElseThrow();
        Long orgId = user.getOrganization() != null ? user.getOrganization().getId() : null;
        if (orgId == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error("No organization"));
        }
        return ResponseEntity.ok(ApiResponse.ok(dayOffTypeService.getActiveForOrg(orgId)));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<DayOffTypeResponse>> create(
            Authentication auth, @Valid @RequestBody DayOffTypeRequest request) {
        User user = userRepository.findByUserName(auth.getName()).orElseThrow();
        Long orgId = user.getOrganization() != null ? user.getOrganization().getId() : null;
        if (orgId == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error("No organization"));
        }
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Day-off type created", dayOffTypeService.create(orgId, request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<DayOffTypeResponse>> update(
            @PathVariable Long id, @Valid @RequestBody DayOffTypeRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Day-off type updated", dayOffTypeService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        dayOffTypeService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok("Day-off type deleted", null));
    }
}
