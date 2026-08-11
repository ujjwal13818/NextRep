package com.nextset.controller;

import com.nextset.enums.Days;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static com.nextset.EndpointConstants.GET_DAYS;
import static com.nextset.EndpointConstants.WORKOUT_BASE_API;

@RestController
@RequestMapping(WORKOUT_BASE_API)
public class GymDay {

    @GetMapping(GET_DAYS)
    public List<Days> getDays() {
        return List.of(Days.values());
    }
}
