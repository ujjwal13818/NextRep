package com.nextset.repository;

import com.nextset.entity.SetLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SetLogRepository extends JpaRepository<SetLog, UUID> {
    List<SetLog> findByWorkoutExerciseLogIdOrderBySetNumberAsc(UUID workoutExerciseLogId);
}