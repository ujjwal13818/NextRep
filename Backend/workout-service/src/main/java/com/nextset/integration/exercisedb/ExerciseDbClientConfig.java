package com.nextset.integration.exercisedb;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class ExerciseDbClientConfig {

    @Bean
    public RestClient exerciseDbRestClient(
            @Value("${exercisedb.base-url}") String baseUrl,
            @Value("${exercisedb.key}") String apiKey,
            @Value("${exercisedb.host}") String host) {

        return RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("X-RapidAPI-Key", apiKey)
                .defaultHeader("X-RapidAPI-Host", host)
                .build();
    }
}