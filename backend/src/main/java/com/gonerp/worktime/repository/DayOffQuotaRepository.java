package com.gonerp.worktime.repository;

import com.gonerp.worktime.model.DayOffQuota;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DayOffQuotaRepository extends JpaRepository<DayOffQuota, Long> {

    List<DayOffQuota> findByUserIdAndYear(Long userId, int year);

    Optional<DayOffQuota> findByUserIdAndDayOffTypeIdAndYear(Long userId, Long typeId, int year);

    List<DayOffQuota> findByDayOffTypeOrganizationIdAndYear(Long orgId, int year);
}
