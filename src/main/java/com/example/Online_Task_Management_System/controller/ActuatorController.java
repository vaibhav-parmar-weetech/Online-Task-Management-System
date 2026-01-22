package com.example.Online_Task_Management_System.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/monitor")
@Tag(name = "Actuator APIs", description = "Health api")
public class ActuatorController {

    private final HealthEndpoint healthEndpoint;

    public ActuatorController(HealthEndpoint healthEndpoint) {
        this.healthEndpoint = healthEndpoint;
    }

    @GetMapping("/health")
    public Object getHealth() {
        return healthEndpoint.health();
    }
}

