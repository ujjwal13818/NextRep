package com.nextset.response;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record SessionSummaryResponse(
        UUID id,
        OffsetDateTime startedAt,
        OffsetDateTime completedAt,
        String status,
        List<ExerciseLogSummary> exercises
) {
    public record ExerciseLogSummary(
            UUID workoutExerciseLogId,
            UUID exerciseId,
            String exerciseName,
            List<SetLogResponse> sets
    ) {}
}