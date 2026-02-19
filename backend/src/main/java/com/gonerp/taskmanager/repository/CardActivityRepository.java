package com.gonerp.taskmanager.repository;

import com.gonerp.taskmanager.model.CardActivity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CardActivityRepository extends JpaRepository<CardActivity, Long> {
}
