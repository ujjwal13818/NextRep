package com.nextset.response;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record SetLogResponse(
        UUID id,
        UUID workoutExerciseLogId,
        int setNumber,
        int reps,
        BigDecimal weight,
        Integer intensity,
        OffsetDateTime loggedAt
) {}