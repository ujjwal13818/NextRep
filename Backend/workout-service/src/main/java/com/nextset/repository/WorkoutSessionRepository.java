package com.nextset.repository;

import com.nextset.entity.WorkoutSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface WorkoutSessionRepository extends JpaRepository<WorkoutSession, UUID> {
    Optional<WorkoutSession> findFirstByUserIdAndStatusOrderByStartedAtDesc(UUID userId, String status);
}