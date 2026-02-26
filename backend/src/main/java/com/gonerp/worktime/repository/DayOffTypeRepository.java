package com.gonerp.worktime.repository;

import com.gonerp.worktime.model.DayOffType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DayOffTypeRepository extends JpaRepository<DayOffType, Long> {

    List<DayOffType> findByOrganizationIdAndActiveOrderByDisplayOrder(Long orgId, boolean active);

    List<DayOffType> findByOrganizationIdOrderByDisplayOrder(Long orgId);
}
