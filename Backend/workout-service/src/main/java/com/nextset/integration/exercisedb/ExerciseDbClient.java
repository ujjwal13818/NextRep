package com.nextset.integration.exercisedb;

import com.nextset.integration.exercisedb.dto.ExerciseDbResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Objects;

@Component
public class ExerciseDbClient {

    private final RestClient restClient;

    public ExerciseDbClient(RestClient exerciseDbRestClient) {
        this.restClient = exerciseDbRestClient;
    }

    public List fetchBodyPartList() {
        return restClient.get()
                .uri("/exercises/bodyPartList")
                .retrieve()
                .body(List.class);
    }

    public List<ExerciseDbResponse> fetchExercisesByBodyPart(String bodyPart) {
        return List.of(Objects.requireNonNull(restClient.get()
                .uri("/exercises/bodyPart/{bodyPart}", bodyPart)
                .retrieve()
                .body(ExerciseDbResponse[].class)));
    }
}