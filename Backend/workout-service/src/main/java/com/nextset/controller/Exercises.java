package com.nextset.controller;

import com.nextset.response.ExerciseResponse;
import com.nextset.service.ExerciseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

import static com.nextset.EndpointConstants.WORKOUT_BASE_API;

@RestController
@RequestMapping(WORKOUT_BASE_API)
public class Exercises {

    @Autowired
    private ExerciseService exerciseService;

    @GetMapping("/body-part/{bodyPartId}/exercises")
    public List<ExerciseResponse> getExercisesByBodyPart(@PathVariable UUID bodyPartId) {
        return exerciseService.getExercisesByBodyPart(bodyPartId);
    }
}