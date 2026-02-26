package com.gonerp.worktime.repository;

import com.gonerp.worktime.model.BreakEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BreakEntryRepository extends JpaRepository<BreakEntry, Long> {
}
