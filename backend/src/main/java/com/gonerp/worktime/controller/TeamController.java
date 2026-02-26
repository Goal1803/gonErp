package com.gonerp.worktime.controller;

import com.gonerp.common.ApiResponse;
import com.gonerp.usermanager.model.User;
import com.gonerp.usermanager.repository.UserRepository;
import com.gonerp.worktime.dto.TeamAvailabilityDTO;
import com.gonerp.worktime.service.TeamAvailabilityService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.Authentication;

@RestController
@RequestMapping("/api/worktime/team")
@RequiredArgsConstructor
public class TeamController {

    private final TeamAvailabilityService teamAvailabilityService;
    private final UserRepository userRepository;

    @GetMapping("/availability")
    public ResponseEntity<ApiResponse<TeamAvailabilityDTO>> getAvailability(Authentication auth) {
        User user = userRepository.findByUserName(auth.getName()).orElseThrow();
        if (user.getOrganization() == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error("No organization"));
        }
        Long orgId = user.getOrganization().getId();
        return ResponseEntity.ok(ApiResponse.ok(teamAvailabilityService.getTeamAvailability(orgId)));
    }
}
