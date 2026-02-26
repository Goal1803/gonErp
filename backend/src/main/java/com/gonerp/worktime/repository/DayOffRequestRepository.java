package com.gonerp.worktime.repository;

import com.gonerp.worktime.model.DayOffRequest;
import com.gonerp.worktime.model.enums.DayOffRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface DayOffRequestRepository extends JpaRepository<DayOffRequest, Long> {

    List<DayOffRequest> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<DayOffRequest> findByOrganizationIdAndStatus(Long orgId, DayOffRequestStatus status);

    List<DayOffRequest> findByOrganizationIdAndStatusIn(Long orgId, List<DayOffRequestStatus> statuses);

    List<DayOffRequest> findByUserIdAndStartDateBetween(Long userId, LocalDate start, LocalDate end);

    List<DayOffRequest> findByOrganizationIdAndStartDateBetweenAndStatus(
            Long orgId, LocalDate start, LocalDate end, DayOffRequestStatus status);

    @Query("SELECT r FROM DayOffRequest r WHERE r.organization.id = :orgId " +
           "AND r.status IN :statuses " +
           "AND r.startDate <= :endDate AND r.endDate >= :startDate")
    List<DayOffRequest> findOverlappingRequests(
            @Param("orgId") Long orgId,
            @Param("statuses") List<DayOffRequestStatus> statuses,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);
}
