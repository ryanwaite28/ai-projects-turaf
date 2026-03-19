package com.turaf.bff.controllers;

import com.turaf.bff.clients.MetricsServiceClient;
import com.turaf.bff.dto.MetricDto;
import com.turaf.bff.dto.RecordMetricRequest;
import com.turaf.bff.security.UserContext;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@RestController
@RequestMapping("/api/v1/metrics")
@RequiredArgsConstructor
public class MetricsController {
    
    private final MetricsServiceClient metricsServiceClient;
    
    @PostMapping
    public Mono<ResponseEntity<MetricDto>> recordMetric(
            @Valid @RequestBody RecordMetricRequest request,
            @RequestParam String organizationId,
            @AuthenticationPrincipal UserContext userContext) {
        log.info("Record metric for experiment: {}", request.getExperimentId());
        return metricsServiceClient.recordMetric(request, userContext.getUserId(), organizationId)
            .map(ResponseEntity::ok)
            .doOnSuccess(response -> log.info("Metric recorded"))
            .doOnError(error -> log.error("Failed to record metric", error));
    }
    
    @GetMapping("/experiments/{experimentId}")
    public Flux<MetricDto> getExperimentMetrics(
            @PathVariable String experimentId,
            @RequestParam String organizationId,
            @AuthenticationPrincipal UserContext userContext) {
        log.info("Get metrics for experiment: {}", experimentId);
        return metricsServiceClient.getExperimentMetrics(experimentId, userContext.getUserId(), organizationId)
            .doOnComplete(() -> log.info("Retrieved metrics"))
            .doOnError(error -> log.error("Failed to get metrics for experiment {}", experimentId, error));
    }
    
    @GetMapping("/{id}")
    public Mono<ResponseEntity<MetricDto>> getMetric(
            @PathVariable String id,
            @RequestParam String organizationId,
            @AuthenticationPrincipal UserContext userContext) {
        log.info("Get metric: {}", id);
        return metricsServiceClient.getMetric(id, userContext.getUserId(), organizationId)
            .map(ResponseEntity::ok)
            .doOnError(error -> log.error("Failed to get metric {}", id, error));
    }
    
    @DeleteMapping("/{id}")
    public Mono<ResponseEntity<Void>> deleteMetric(
            @PathVariable String id,
            @RequestParam String organizationId,
            @AuthenticationPrincipal UserContext userContext) {
        log.info("Delete metric: {}", id);
        return metricsServiceClient.deleteMetric(id, userContext.getUserId(), organizationId)
            .map(ResponseEntity::ok)
            .doOnSuccess(response -> log.info("Metric deleted"))
            .doOnError(error -> log.error("Failed to delete metric {}", id, error));
    }
}
