package com.gonerp.worktime.repository;

import com.gonerp.worktime.model.PublicHoliday;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface PublicHolidayRepository extends JpaRepository<PublicHoliday, Long> {

    List<PublicHoliday> findByOrganizationId(Long orgId);

    List<PublicHoliday> findByOrganizationIdAndHolidayDateBetween(Long orgId, LocalDate start, LocalDate end);

    List<PublicHoliday> findByOrganizationIdAndIsRecurring(Long orgId, boolean recurring);
}
