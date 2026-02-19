package com.gonerp.taskmanager.repository;

import com.gonerp.taskmanager.model.CardMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CardMemberRepository extends JpaRepository<CardMember, Long> {

    boolean existsByCardIdAndUserId(Long cardId, Long userId);

    Optional<CardMember> findByCardIdAndUserId(Long cardId, Long userId);
}
