package com.nextset.integration.exercisedb;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class SeederRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(SeederRunner.class);

    private final ExerciseSeederService seeder;

    @Value("${app.seed.enabled:false}")
    private boolean enabled;

    public SeederRunner(ExerciseSeederService seeder) {
        this.seeder = seeder;
    }

    @Override
    public void run(String... args) {
        if (!enabled) {
            log.info("Seeding disabled (app.seed.enabled=false)");
            return;
        }

        try {
            log.info("Starting exercise DB seeding...");
            seeder.seedAll();
            log.info("Exercise DB seeding completed.");
        } catch (Exception e) {
            // Log the exception so it doesn't silently rollback without trace
            log.error("Exercise DB seeding failed", e);
        }
    }
}
