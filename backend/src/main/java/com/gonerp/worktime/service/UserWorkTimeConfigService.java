package com.gonerp.worktime.service;

import com.gonerp.usermanager.model.User;
import com.gonerp.usermanager.repository.UserRepository;
import com.gonerp.worktime.dto.UserWorkTimeConfigRequest;
import com.gonerp.worktime.dto.UserWorkTimeConfigResponse;
import com.gonerp.worktime.model.UserWorkTimeConfig;
import com.gonerp.worktime.model.WorkTimeSettings;
import com.gonerp.worktime.repository.UserWorkTimeConfigRepository;
import com.gonerp.worktime.repository.WorkTimeSettingsRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneId;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class UserWorkTimeConfigService {

    private final UserWorkTimeConfigRepository configRepository;
    private final WorkTimeSettingsRepository settingsRepository;
    private final UserRepository userRepository;

    /**
     * Get or lazily create a user's work time config.
     * If none exists, creates one with defaults from org settings.
     */
    public UserWorkTimeConfig getOrCreateConfig(User user) {
        return configRepository.findByUserId(user.getId())
                .orElseGet(() -> createDefaultConfig(user));
    }

    public UserWorkTimeConfig getOrCreateConfig(Long userId) {
        return configRepository.findByUserId(userId)
                .orElseGet(() -> {
                    User user = userRepository.findById(userId)
                            .orElseThrow(() -> new EntityNotFoundException("User not found: " + userId));
                    return createDefaultConfig(user);
                });
    }

    @Transactional(readOnly = true)
    public UserWorkTimeConfigResponse getConfigResponse(Long userId) {
        UserWorkTimeConfig config = getOrCreateConfig(userId);
        return UserWorkTimeConfigResponse.from(config);
    }

    @Transactional(readOnly = true)
    public List<UserWorkTimeConfigResponse> getOrgConfigs(Long orgId) {
        // Ensure all org users have configs
        List<User> orgUsers = userRepository.findByOrganizationId(orgId);
        for (User user : orgUsers) {
            getOrCreateConfig(user);
        }
        return configRepository.findByUserOrganizationId(orgId).stream()
                .map(UserWorkTimeConfigResponse::from)
                .toList();
    }

    public UserWorkTimeConfigResponse updateConfig(Long userId, UserWorkTimeConfigRequest request) {
        UserWorkTimeConfig config = getOrCreateConfig(userId);

        if (request.getTimezoneId() != null) {
            ZoneId.of(request.getTimezoneId()); // validate
            config.setTimezoneId(request.getTimezoneId());
        }
        if (request.getWorkStartTime() != null) config.setWorkStartTime(request.getWorkStartTime());
        if (request.getWorkEndTime() != null) config.setWorkEndTime(request.getWorkEndTime());
        if (request.getDailyWorkingHours() != null) config.setDailyWorkingHours(request.getDailyWorkingHours());
        if (request.getWeeklyWorkingHours() != null) config.setWeeklyWorkingHours(request.getWeeklyWorkingHours());

        return UserWorkTimeConfigResponse.from(configRepository.save(config));
    }

    private UserWorkTimeConfig createDefaultConfig(User user) {
        UserWorkTimeConfig.UserWorkTimeConfigBuilder builder = UserWorkTimeConfig.builder().user(user);

        // Copy defaults from org settings if available
        if (user.getOrganization() != null) {
            settingsRepository.findByOrganizationId(user.getOrganization().getId())
                    .ifPresent(orgSettings -> {
                        builder.timezoneId(orgSettings.getTimezoneId());
                        builder.workStartTime(orgSettings.getWorkStartTime());
                        builder.workEndTime(orgSettings.getWorkEndTime());
                        builder.dailyWorkingHours(orgSettings.getDailyWorkingHours());
                        builder.weeklyWorkingHours(orgSettings.getWeeklyFullTimeHours());
                    });
        }

        return configRepository.save(builder.build());
    }
}
