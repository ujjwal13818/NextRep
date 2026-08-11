package com.nextset.response;

import java.util.UUID;

public class ExerciseResponse {
    private UUID id;
    private String name;
    private String equipment;
    private String difficulty;
    private String instructions;
    private String demoUrl;

    public ExerciseResponse() {}

    public ExerciseResponse(UUID id, String name, String equipment, String difficulty,
                            String instructions, String demoUrl) {
        this.id = id;
        this.name = name;
        this.equipment = equipment;
        this.difficulty = difficulty;
        this.instructions = instructions;
        this.demoUrl = demoUrl;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEquipment() { return equipment; }
    public void setEquipment(String equipment) { this.equipment = equipment; }

    public String getDifficulty() { return difficulty; }
    public void setDifficulty(String difficulty) { this.difficulty = difficulty; }

    public String getInstructions() { return instructions; }
    public void setInstructions(String instructions) { this.instructions = instructions; }

    public String getDemoUrl() { return demoUrl; }
    public void setDemoUrl(String demoUrl) { this.demoUrl = demoUrl; }
}