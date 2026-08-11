package com.nextset.integration.exercisedb.dto;

import java.util.List;

public record ExerciseDbResponse(
        String id,
        String name,
        String bodyPart,
        String target,
        String equipment,
        List<String> secondaryMuscles,
        List<String> instructions,
        String description,
        String difficulty,
        String category
) {}