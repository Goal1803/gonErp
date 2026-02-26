package com.gonerp.worktime.service;

import com.gonerp.worktime.model.DayOffQuota;
import com.gonerp.worktime.model.WorkTimeSettings;
import com.gonerp.worktime.repository.DayOffQuotaRepository;
import com.gonerp.worktime.repository.WorkTimeSettingsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class YearEndCarryOverScheduler {

    private final WorkTimeSettingsRepository settingsRepository;
    private final DayOffQuotaRepository quotaRepository;

    @Scheduled(cron = "0 0 0 1 1 *")
    @Transactional
    public void processYearEndCarryOver() {
        int newYear = LocalDate.now().getYear();
        int previousYear = newYear - 1;

        List<WorkTimeSettings> allSettings = settingsRepository.findAll();
        for (WorkTimeSettings settings : allSettings) {
            if (!settings.isCarryOverEnabled()) continue;

            Long orgId = settings.getOrganization().getId();
            int maxCarry = settings.getMaxCarryOverDays();

            List<DayOffQuota> previousQuotas = quotaRepository.findByDayOffTypeOrganizationIdAndYear(orgId, previousYear);
            for (DayOffQuota oldQuota : previousQuotas) {
                double unused = oldQuota.getTotalDays() + oldQuota.getCarriedOverDays() - oldQuota.getUsedDays();
                double carryOver = Math.min(Math.max(unused, 0), maxCarry);
                if (carryOver <= 0) continue;

                DayOffQuota newQuota = quotaRepository.findByUserIdAndDayOffTypeIdAndYear(
                        oldQuota.getUser().getId(), oldQuota.getDayOffType().getId(), newYear
                ).orElseGet(() -> DayOffQuota.builder()
                        .user(oldQuota.getUser())
                        .dayOffType(oldQuota.getDayOffType())
                        .year(newYear)
                        .build()
                );

                newQuota.setCarriedOverDays(carryOver);
                quotaRepository.save(newQuota);
                log.info("Carried over {} days for user {} type {} from {} to {}",
                        carryOver, oldQuota.getUser().getUserName(), oldQuota.getDayOffType().getName(),
                        previousYear, newYear);
            }
        }
    }
}
