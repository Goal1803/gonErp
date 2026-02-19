package com.gonerp.taskmanager.repository;

import com.gonerp.taskmanager.model.CardAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CardAttachmentRepository extends JpaRepository<CardAttachment, Long> {
}
