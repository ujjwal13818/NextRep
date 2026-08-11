package com.nextset;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;


@SpringBootApplication
public class Workout {
    public static void main(String[] args) {
        System.out.println("PGSSLMODE = [" + System.getenv("PGSSLMODE") + "]");
        SpringApplication.run(Workout.class, args);
    }
}


