package com.nextset.repository;

import com.nextset.entity.ExerciseBodyPart;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExerciseBodyPartRepository
        extends JpaRepository<ExerciseBodyPart, ExerciseBodyPart.ExerciseBodyPartId> {
}