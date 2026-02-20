package com.gonerp.taskmanager.repository;

import com.gonerp.taskmanager.model.CardLink;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CardLinkRepository extends JpaRepository<CardLink, Long> {
}
