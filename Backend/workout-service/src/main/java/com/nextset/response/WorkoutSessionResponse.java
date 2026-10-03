package com.nextset.response;

import java.time.OffsetDateTime;
import java.util.UUID;

public record WorkoutSessionResponse(
        UUID id,
        UUID userId,
        OffsetDateTime startedAt,
        OffsetDateTime completedAt,
        String status
) {}