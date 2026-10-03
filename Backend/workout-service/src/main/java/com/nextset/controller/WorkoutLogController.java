package com.nextset.controller;

import com.nextset.entity.WorkoutSession;
import com.nextset.response.SessionSummaryResponse;
import com.nextset.response.SetLogResponse;
import com.nextset.response.WorkoutExerciseLogResponse;
import com.nextset.entity.WorkoutExerciseLog;
import com.nextset.entity.SetLog;
import com.nextset.response.WorkoutSessionResponse;
import com.nextset.service.WorkoutLogService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static com.nextset.EndpointConstants.WORKOUT_BASE_API;

@RestController
@RequestMapping(WORKOUT_BASE_API)
public class WorkoutLogController {

    private final WorkoutLogService workoutLogService;

    public WorkoutLogController(WorkoutLogService workoutLogService) {
        this.workoutLogService = workoutLogService;
    }

    @PostMapping("/exercise-log/{exerciseId}/start")
    public WorkoutExerciseLogResponse startExerciseLog(@AuthenticationPrincipal String email,
                                                       @PathVariable UUID exerciseId) {
        WorkoutExerciseLog log = workoutLogService.startExerciseLog(email, exerciseId);
        return new WorkoutExerciseLogResponse(
                log.getId(),
                log.getSession().getId(),
                log.getExercise().getId(),
                log.getExercise().getName(),
                log.getCreatedAt()
        );
    }

    @PostMapping("/exercise-log/{workoutExerciseLogId}/sets")
    public SetLogResponse logSet(@PathVariable UUID workoutExerciseLogId,
                                 @RequestBody LogSetRequest request) {
        SetLog setLog = workoutLogService.logSet(
                workoutExerciseLogId,
                request.setNumber(),
                request.reps(),
                request.weight(),
                request.intensity()
        );

        return new SetLogResponse(
                setLog.getId(),
                workoutExerciseLogId,
                setLog.getSetNumber(),
                setLog.getReps(),
                setLog.getWeight(),
                setLog.getIntensity(),
                setLog.getLoggedAt()
        );
    }

    @GetMapping("/session/{sessionId}/summary")
    public SessionSummaryResponse getSessionSummary(@PathVariable UUID sessionId) {
        WorkoutSession session = workoutLogService.getSession(sessionId);
        List<WorkoutExerciseLog> exerciseLogs = workoutLogService.getExerciseLogsForSession(sessionId);

        List<SessionSummaryResponse.ExerciseLogSummary> exerciseSummaries = exerciseLogs.stream()
                .map(log -> {
                    List<SetLogResponse> sets = workoutLogService.getSetsForExerciseLog(log.getId())
                            .stream()
                            .map(setLog -> new SetLogResponse(
                                    setLog.getId(),
                                    log.getId(),
                                    setLog.getSetNumber(),
                                    setLog.getReps(),
                                    setLog.getWeight(),
                                    setLog.getIntensity(),
                                    setLog.getLoggedAt()
                            ))
                            .toList();

                    return new SessionSummaryResponse.ExerciseLogSummary(
                            log.getId(),
                            log.getExercise().getId(),
                            log.getExercise().getName(),
                            sets
                    );
                })
                .toList();

        return new SessionSummaryResponse(
                session.getId(),
                session.getStartedAt(),
                session.getCompletedAt(),
                session.getStatus(),
                exerciseSummaries
        );
    }

    @PostMapping("/session/complete-current")
    public ResponseEntity<WorkoutSessionResponse> completeCurrentSession(@AuthenticationPrincipal String email) {
        return workoutLogService.completeCurrentSessionIfAny(email)
                .map(session -> ResponseEntity.ok(new WorkoutSessionResponse(
                        session.getId(),
                        session.getUserId(),
                        session.getStartedAt(),
                        session.getCompletedAt(),
                        session.getStatus()
                )))
                .orElse(ResponseEntity.noContent().build()); // nothing was in progress — fine
    }

    @PostMapping("/session/{sessionId}/complete")
    public WorkoutSessionResponse completeSession(@PathVariable UUID sessionId) {
        WorkoutSession session = workoutLogService.completeSession(sessionId);
        return new WorkoutSessionResponse(
                session.getId(),
                session.getUserId(),
                session.getStartedAt(),
                session.getCompletedAt(),
                session.getStatus()
        );
    }

    @GetMapping("/session/current")
    public ResponseEntity<WorkoutSessionResponse> getCurrentSession(@AuthenticationPrincipal String email) {
        return workoutLogService.getCurrentSessionIfAny(email)
                .map(session -> ResponseEntity.ok(new WorkoutSessionResponse(
                        session.getId(),
                        session.getUserId(),
                        session.getStartedAt(),
                        session.getCompletedAt(),
                        session.getStatus()
                )))
                .orElse(ResponseEntity.noContent().build());
    }

    public record LogSetRequest(int setNumber, int reps, BigDecimal weight, Integer intensity) {}
}