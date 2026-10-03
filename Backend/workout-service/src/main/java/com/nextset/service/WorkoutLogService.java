package com.nextset.service;

import com.nextset.entity.*;
import com.nextset.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class WorkoutLogService {

    private final WorkoutSessionRepository sessionRepository;
    private final WorkoutExerciseLogRepository exerciseLogRepository;
    private final SetLogRepository setLogRepository;
    private final ExerciseRepository exerciseRepository;
    private final UserRepository userRepository;

    public WorkoutLogService(WorkoutSessionRepository sessionRepository,
                             WorkoutExerciseLogRepository exerciseLogRepository,
                             SetLogRepository setLogRepository,
                             ExerciseRepository exerciseRepository,
                             UserRepository userRepository) {
        this.sessionRepository = sessionRepository;
        this.exerciseLogRepository = exerciseLogRepository;
        this.setLogRepository = setLogRepository;
        this.exerciseRepository = exerciseRepository;
        this.userRepository = userRepository;
    }

    private UUID resolveUserId(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("No user found for email: " + email))
                .getId();
    }

    @Transactional
    public WorkoutSession getOrCreateTodaysSession(String email) {
        UUID userId = resolveUserId(email);

        return sessionRepository
                .findFirstByUserIdAndStatusOrderByStartedAtDesc(userId, "IN_PROGRESS")
                .orElseGet(() -> {
                    WorkoutSession session = new WorkoutSession();
                    session.setUserId(userId);
                    session.setStartedAt(OffsetDateTime.now());
                    session.setStatus("IN_PROGRESS");
                    return sessionRepository.save(session);
                });
    }

    @Transactional
    public WorkoutExerciseLog startExerciseLog(String email, UUID exerciseId) {
        WorkoutSession session = getOrCreateTodaysSession(email);

        Exercise exercise = exerciseRepository.findById(exerciseId)
                .orElseThrow(() -> new IllegalArgumentException("Exercise not found: " + exerciseId));

        WorkoutExerciseLog log = new WorkoutExerciseLog();
        log.setSession(session);
        log.setExercise(exercise);
        log.setCreatedAt(OffsetDateTime.now());
        return exerciseLogRepository.save(log);
    }

    @Transactional
    public SetLog logSet(UUID workoutExerciseLogId, int setNumber, int reps,
                         BigDecimal weight, Integer intensity) {
        WorkoutExerciseLog exerciseLog = exerciseLogRepository.findById(workoutExerciseLogId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Workout exercise log not found: " + workoutExerciseLogId));

        SetLog setLog = new SetLog();
        setLog.setWorkoutExerciseLog(exerciseLog);
        setLog.setSetNumber(setNumber);
        setLog.setReps(reps);
        setLog.setWeight(weight);
        setLog.setIntensity(intensity);
        setLog.setLoggedAt(OffsetDateTime.now());

        return setLogRepository.save(setLog);

        // Kafka set.logged publish goes here once this flow is confirmed working
    }

    @Transactional
    public WorkoutSession completeSession(UUID sessionId) {
        WorkoutSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Session not found: " + sessionId));

        session.setStatus("COMPLETED");
        session.setCompletedAt(OffsetDateTime.now());
        return sessionRepository.save(session);

        // Kafka workout.completed publish goes here once this flow is confirmed working
    }

    @Transactional(readOnly = true)
    public WorkoutSession getSession(UUID sessionId) {
        return sessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Session not found: " + sessionId));
    }

    @Transactional(readOnly = true)
    public List<WorkoutExerciseLog> getExerciseLogsForSession(UUID sessionId) {
        return exerciseLogRepository.findBySessionIdOrderByCreatedAtAsc(sessionId);
    }

    @Transactional(readOnly = true)
    public List<SetLog> getSetsForExerciseLog(UUID workoutExerciseLogId) {
        return setLogRepository.findByWorkoutExerciseLogIdOrderBySetNumberAsc(workoutExerciseLogId);
    }

    @Transactional
    public Optional<WorkoutSession> completeCurrentSessionIfAny(String email) {
        UUID userId = resolveUserId(email);

        return sessionRepository
                .findFirstByUserIdAndStatusOrderByStartedAtDesc(userId, "IN_PROGRESS")
                .map(session -> {
                    session.setStatus("COMPLETED");
                    session.setCompletedAt(OffsetDateTime.now());
                    return sessionRepository.save(session);
                });
    }

    @Transactional(readOnly = true)
    public Optional<WorkoutSession> getCurrentSessionIfAny(String email) {
        UUID userId = resolveUserId(email);
        return sessionRepository.findFirstByUserIdAndStatusOrderByStartedAtDesc(userId, "IN_PROGRESS");
    }
}