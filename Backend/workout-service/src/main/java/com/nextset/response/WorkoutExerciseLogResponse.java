package com.nextset.response;

import java.time.OffsetDateTime;
import java.util.UUID;

public record WorkoutExerciseLogResponse(
        UUID id,
        UUID sessionId,
        UUID exerciseId,
        String exerciseName,
        OffsetDateTime createdAt
) {}