package com.nextset.entity;

import jakarta.persistence.*;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "exercise_body_part")
@IdClass(ExerciseBodyPart.ExerciseBodyPartId.class)
public class ExerciseBodyPart {

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exercise_id")
    private Exercise exercise;

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "body_part_id")
    private BodyPart bodyPart;

    @Column(name = "is_primary", nullable = false)
    private boolean isPrimary = true;

    public Exercise getExercise() { return exercise; }
    public void setExercise(Exercise exercise) { this.exercise = exercise; }

    public BodyPart getBodyPart() { return bodyPart; }
    public void setBodyPart(BodyPart bodyPart) { this.bodyPart = bodyPart; }

    public boolean isPrimary() { return isPrimary; }
    public void setPrimary(boolean primary) { isPrimary = primary; }

    // Composite key class — required since this entity has no single-column PK
    public static class ExerciseBodyPartId implements Serializable {
        private UUID exercise;
        private UUID bodyPart;

        public ExerciseBodyPartId() {}

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof ExerciseBodyPartId that)) return false;
            return Objects.equals(exercise, that.exercise) && Objects.equals(bodyPart, that.bodyPart);
        }

        @Override
        public int hashCode() {
            return Objects.hash(exercise, bodyPart);
        }
    }
}