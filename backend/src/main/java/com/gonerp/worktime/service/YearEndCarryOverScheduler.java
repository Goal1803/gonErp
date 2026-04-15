package com.gonerp.worktime.service;

import com.gonerp.usermanager.model.User;
import com.gonerp.usermanager.repository.UserRepository;
import com.gonerp.worktime.model.DayOffQuota;
import com.gonerp.worktime.model.DayOffType;
import com.gonerp.worktime.repository.DayOffQuotaRepository;
import com.gonerp.worktime.repository.DayOffTypeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Runs at 00:00 on January 1 to seed each user's new-year quotas from the active
 * day-off types' defaultQuota. No carry-over: unused days from the previous year are
 * not transferred.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class YearEndCarryOverScheduler {

    private final DayOffQuotaRepository quotaRepository;
    private final DayOffTypeRepository dayOffTypeRepository;
    private final UserRepository userRepository;

    @Scheduled(cron = "0 0 0 1 1 *")
    @Transactional
    public void resetAnnualQuotas() {
        int newYear = LocalDate.now().getYear();
        List<DayOffType> types = dayOffTypeRepository.findAll();
        int created = 0;

        for (DayOffType type : types) {
            if (!type.isActive() || type.getOrganization() == null) continue;
            Long orgId = type.getOrganization().getId();
            List<User> users = userRepository.findByOrganizationId(orgId);
            for (User u : users) {
                boolean exists = quotaRepository
                        .findByUserIdAndDayOffTypeIdAndYear(u.getId(), type.getId(), newYear)
                        .isPresent();
                if (exists) continue;
                DayOffQuota q = DayOffQuota.builder()
                        .user(u)
                        .dayOffType(type)
                        .year(newYear)
                        .totalDays(type.getDefaultQuota())
                        .usedDays(0)
                        .carriedOverDays(0)
                        .build();
                quotaRepository.save(q);
                created++;
            }
        }
        log.info("Annual quota reset complete for {}: created {} new quota rows", newYear, created);
    }
}
