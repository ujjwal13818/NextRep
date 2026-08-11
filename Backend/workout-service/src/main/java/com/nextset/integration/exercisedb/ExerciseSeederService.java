package com.nextset.integration.exercisedb;

import com.nextset.entity.BodyPart;
import com.nextset.entity.Exercise;
import com.nextset.entity.ExerciseBodyPart;
import com.nextset.integration.exercisedb.dto.ExerciseDbResponse;
import com.nextset.repository.BodyPartRepository;
import com.nextset.repository.ExerciseBodyPartRepository;
import com.nextset.repository.ExerciseRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ExerciseSeederService {

    private static final Logger log = LoggerFactory.getLogger(ExerciseSeederService.class);

    private final ExerciseDbClient client;
    private final BodyPartRepository bodyPartRepository;
    private final ExerciseRepository exerciseRepository;
    private final ExerciseBodyPartRepository exerciseBodyPartRepository;

    public ExerciseSeederService(ExerciseDbClient client,
                                 BodyPartRepository bodyPartRepository,
                                 ExerciseRepository exerciseRepository,
                                 ExerciseBodyPartRepository exerciseBodyPartRepository) {
        this.client = client;
        this.bodyPartRepository = bodyPartRepository;
        this.exerciseRepository = exerciseRepository;
        this.exerciseBodyPartRepository = exerciseBodyPartRepository;
    }

    @Transactional
    public void seedAll() {
        List<String> bodyParts = client.fetchBodyPartList();
        Map<String, BodyPart> bodyPartCache = new HashMap<>();

        // 1. Seed body_part table first
        for (String bp : bodyParts) {
            BodyPart entity = bodyPartRepository.findByName(bp.toUpperCase())
                    .orElseGet(() -> {
                        BodyPart newBp = new BodyPart();
                        newBp.setName(bp.toUpperCase());
                        newBp.setDisplayName(capitalize(bp));
                        return bodyPartRepository.save(newBp);
                    });
            bodyPartCache.put(bp, entity);
        }
        log.info("Seeded {} body parts", bodyPartCache.size());

        // 2. Fetch + seed exercises per body part
        int exerciseCount = 0;
        for (String bp : bodyParts) {
            List<ExerciseDbResponse> exercises = client.fetchExercisesByBodyPart(bp);

            for (ExerciseDbResponse dto : exercises) {
                if (exerciseRepository.existsByExternalId(dto.id())) {
                    continue; // already seeded, skip (safe to re-run)
                }

                Exercise exercise = new Exercise();
                exercise.setExternalId(dto.id());
                exercise.setName(dto.name());
                exercise.setEquipment(dto.equipment());
                exercise.setDifficulty(dto.difficulty());
                exercise.setInstructions(dto.instructions() == null ? null
                        : String.join(" ", dto.instructions()));
                exercise.setCreatedAt(OffsetDateTime.now());
                // demo_url intentionally left null — served via proxy endpoint using external_id
                exerciseRepository.save(exercise);

                // Link to primary body part
                linkExerciseToBodyPart(exercise, bodyPartCache.get(bp), true);

                exerciseCount++;
            }
        }
        log.info("Seeded {} exercises", exerciseCount);
    }

    private void linkExerciseToBodyPart(Exercise exercise, BodyPart bodyPart, boolean isPrimary) {
        ExerciseBodyPart link = new ExerciseBodyPart();
        link.setExercise(exercise);
        link.setBodyPart(bodyPart);
        link.setPrimary(isPrimary);
        exerciseBodyPartRepository.save(link);
    }

    private String capitalize(String s) {
        return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}