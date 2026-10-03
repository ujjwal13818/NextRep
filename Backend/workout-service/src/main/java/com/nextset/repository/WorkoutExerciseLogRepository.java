package com.nextset.repository;

import com.nextset.entity.WorkoutExerciseLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface WorkoutExerciseLogRepository extends JpaRepository<WorkoutExerciseLog, UUID> {
    List<WorkoutExerciseLog> findBySessionIdOrderByCreatedAtAsc(UUID sessionId);
}