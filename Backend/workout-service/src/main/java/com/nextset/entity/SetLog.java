package com.nextset.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "set_log")
public class SetLog {

    @Id
    @GeneratedValue
    private UUID id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workout_exercise_log_id", nullable = false)
    private WorkoutExerciseLog workoutExerciseLog;

    @Column(name = "set_number", nullable = false)
    private Integer setNumber;

    @Column(nullable = false)
    private Integer reps;

    private java.math.BigDecimal weight;

    private Integer intensity; // 1-5

    @Column(name = "logged_at", nullable = false)
    private OffsetDateTime loggedAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public WorkoutExerciseLog getWorkoutExerciseLog() { return workoutExerciseLog; }
    public void setWorkoutExerciseLog(WorkoutExerciseLog workoutExerciseLog) { this.workoutExerciseLog = workoutExerciseLog; }

    public Integer getSetNumber() { return setNumber; }
    public void setSetNumber(Integer setNumber) { this.setNumber = setNumber; }

    public Integer getReps() { return reps; }
    public void setReps(Integer reps) { this.reps = reps; }

    public java.math.BigDecimal getWeight() { return weight; }
    public void setWeight(java.math.BigDecimal weight) { this.weight = weight; }

    public Integer getIntensity() { return intensity; }
    public void setIntensity(Integer intensity) { this.intensity = intensity; }

    public OffsetDateTime getLoggedAt() { return loggedAt; }
    public void setLoggedAt(OffsetDateTime loggedAt) { this.loggedAt = loggedAt; }
}