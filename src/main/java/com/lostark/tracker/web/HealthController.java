package com.lostark.tracker.web;

import com.lostark.tracker.health.CollectionHealthService;
import com.lostark.tracker.web.dto.CollectionHealthResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The collection-pipeline ops surface (OPS-01): {@code GET /api/health/collection} returns the latest
 * run's timestamps, counts, status, and categorical marker — proving the pipeline is alive — without
 * ever exposing the API key or any secret (D-12). Distinct from Spring Boot Actuator's
 * {@code /actuator/health}, which covers infra liveness.
 */
@RestController
public class HealthController {

    private final CollectionHealthService collectionHealthService;

    public HealthController(CollectionHealthService collectionHealthService) {
        this.collectionHealthService = collectionHealthService;
    }

    @GetMapping("/api/health/collection")
    public CollectionHealthResponse collection() {
        return collectionHealthService.latestHealth();
    }
}
