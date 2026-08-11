package com.nextset.repository;

import com.nextset.entity.Exercise;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ExerciseRepository extends JpaRepository<Exercise, UUID> {

    boolean existsByExternalId(String externalId);

    @Query("""
        SELECT e FROM Exercise e
        JOIN ExerciseBodyPart ebp ON ebp.exercise.id = e.id
        WHERE ebp.bodyPart.id = :bodyPartId
    """)
    List<Exercise> findByBodyPartId(@Param("bodyPartId") UUID bodyPartId);
}