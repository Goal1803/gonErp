package com.gonerp.worktime.repository;

import com.gonerp.worktime.model.TimeEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.gonerp.worktime.model.enums.TimeEntryStatus;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface TimeEntryRepository extends JpaRepository<TimeEntry, Long> {
    Optional<TimeEntry> findByUserIdAndWorkDate(Long userId, LocalDate workDate);

    List<TimeEntry> findByUserIdAndWorkDateBetweenOrderByWorkDateDesc(Long userId, LocalDate start, LocalDate end);

    List<TimeEntry> findByOrganizationIdAndWorkDate(Long organizationId, LocalDate workDate);

    List<TimeEntry> findByOrganizationIdAndWorkDateBetween(Long organizationId, LocalDate start, LocalDate end);

    List<TimeEntry> findByOrganizationIdAndWorkDateAndStatusIn(Long organizationId, LocalDate workDate, List<TimeEntryStatus> statuses);

    List<TimeEntry> findByOrganizationIdAndWorkDateAndDailyNotesIsNotNullOrderByUserUserNameAsc(Long organizationId, LocalDate workDate);

    List<TimeEntry> findByUserIdAndDailyNotesIsNotNullAndWorkDateBetweenOrderByWorkDateDesc(Long userId, LocalDate start, LocalDate end);
}
