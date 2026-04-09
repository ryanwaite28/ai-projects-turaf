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

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/metrics")
@RequiredArgsConstructor
public class MetricsController {
    
    private final MetricsServiceClient metricsServiceClient;
    
    @PostMapping
    public ResponseEntity<MetricDto> recordMetric(
            @Valid @RequestBody RecordMetricRequest request,
            @AuthenticationPrincipal UserContext userContext) {
        log.info("Record metric for experiment: {}", request.getExperimentId());
        String organizationId = userContext.getOrganizationId();
        MetricDto metric = metricsServiceClient.recordMetric(request, userContext.getUserId(), organizationId);
        log.info("Metric recorded");
        return ResponseEntity.ok(metric);
    }
    
    @GetMapping("/experiments/{experimentId}")
    public List<MetricDto> getExperimentMetrics(
            @PathVariable String experimentId,
            @AuthenticationPrincipal UserContext userContext) {
        log.info("Get metrics for experiment: {}", experimentId);
        String organizationId = userContext.getOrganizationId();
        List<MetricDto> metrics = metricsServiceClient.getExperimentMetrics(experimentId, userContext.getUserId(), organizationId);
        log.info("Retrieved metrics");
        return metrics;
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<MetricDto> getMetric(
            @PathVariable String id,
            @AuthenticationPrincipal UserContext userContext) {
        log.info("Get metric: {}", id);
        String organizationId = userContext.getOrganizationId();
        MetricDto metric = metricsServiceClient.getMetric(id, userContext.getUserId(), organizationId);
        log.info("Retrieved metric");
        return ResponseEntity.ok(metric);
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMetric(
            @PathVariable String id,
            @AuthenticationPrincipal UserContext userContext) {
        log.info("Delete metric: {}", id);
        String organizationId = userContext.getOrganizationId();
        metricsServiceClient.deleteMetric(id, userContext.getUserId(), organizationId);
        log.info("Metric deleted");
        return ResponseEntity.ok().build();
    }
}
