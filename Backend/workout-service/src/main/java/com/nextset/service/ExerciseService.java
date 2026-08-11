package com.nextset.service;

import com.nextset.response.ExerciseResponse;
import com.nextset.entity.Exercise;
import com.nextset.repository.ExerciseRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ExerciseService {

    private final ExerciseRepository exerciseRepository;

    public ExerciseService(ExerciseRepository exerciseRepository) {
        this.exerciseRepository = exerciseRepository;
    }

    public List<ExerciseResponse> getExercisesByBodyPart(UUID bodyPartId) {
        List<Exercise> exercises = exerciseRepository.findByBodyPartId(bodyPartId);

        return exercises.stream()
                .map(e -> new ExerciseResponse(
                        e.getId(),
                        e.getName(),
                        e.getEquipment(),
                        e.getDifficulty(),
                        e.getInstructions(),
                        e.getDemoUrl()
                ))
                .collect(Collectors.toList());
    }
}