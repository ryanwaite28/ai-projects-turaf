package com.turaf.metrics.interfaces.rest;

import com.turaf.metrics.application.AggregationService;
import com.turaf.metrics.application.BatchMetricService;
import com.turaf.metrics.application.MetricService;
import com.turaf.metrics.application.dto.AggregatedMetricsDto;
import com.turaf.metrics.application.dto.BatchRecordRequest;
import com.turaf.metrics.application.dto.MetricDto;
import com.turaf.metrics.application.dto.RecordMetricRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/metrics")
@PreAuthorize("isAuthenticated()")
@Tag(name = "Metrics", description = "Metric recording and retrieval API")
public class MetricController {

    private final MetricService metricService;
    private final AggregationService aggregationService;
    private final BatchMetricService batchMetricService;

    public MetricController(MetricService metricService,
                           AggregationService aggregationService,
                           BatchMetricService batchMetricService) {
        this.metricService = metricService;
        this.aggregationService = aggregationService;
        this.batchMetricService = batchMetricService;
    }

    @PostMapping
    @Operation(summary = "Record a single metric", description = "Records a single metric data point for an experiment")
    public ResponseEntity<MetricDto> recordMetric(@Valid @RequestBody RecordMetricRequest request) {
        MetricDto metric = metricService.recordMetric(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(metric);
    }

    @PostMapping("/batch")
    @Operation(summary = "Record batch metrics", description = "Records multiple metrics in a single batch operation")
    public ResponseEntity<Void> recordBatch(@Valid @RequestBody BatchRecordRequest request) {
        batchMetricService.recordBatch(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping
    @Operation(summary = "Get metrics", description = "Retrieves metrics for an experiment within a time range")
    public ResponseEntity<List<MetricDto>> getMetrics(
            @Parameter(description = "Experiment ID") @RequestParam String experimentId,
            @Parameter(description = "Metric name (optional)") @RequestParam(required = false) String name,
            @Parameter(description = "Start time") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant start,
            @Parameter(description = "End time") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant end) {
        List<MetricDto> metrics = metricService.getMetrics(experimentId, name, start, end);
        return ResponseEntity.ok(metrics);
    }

    @GetMapping("/aggregate")
    @Operation(summary = "Aggregate metrics", description = "Calculates aggregated statistics for a specific metric")
    public ResponseEntity<AggregatedMetricsDto> aggregateMetrics(
            @Parameter(description = "Experiment ID") @RequestParam String experimentId,
            @Parameter(description = "Metric name") @RequestParam String name,
            @Parameter(description = "Start time") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant start,
            @Parameter(description = "End time") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant end) {
        AggregatedMetricsDto aggregated = aggregationService.aggregateMetrics(
            experimentId, name, start, end
        );
        return ResponseEntity.ok(aggregated);
    }

    @GetMapping("/aggregate/all")
    @Operation(summary = "Aggregate all metrics", description = "Calculates aggregated statistics for all metrics in an experiment")
    public ResponseEntity<Map<String, AggregatedMetricsDto>> aggregateAllMetrics(
            @Parameter(description = "Experiment ID") @RequestParam String experimentId,
            @Parameter(description = "Start time") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant start,
            @Parameter(description = "End time") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant end) {
        Map<String, AggregatedMetricsDto> aggregated = aggregationService.aggregateAllMetrics(
            experimentId, start, end
        );
        return ResponseEntity.ok(aggregated);
    }

    @DeleteMapping("/experiment/{experimentId}")
    @Operation(summary = "Delete metrics by experiment", description = "Deletes all metrics for a specific experiment")
    public ResponseEntity<Void> deleteMetricsByExperiment(
            @Parameter(description = "Experiment ID") @PathVariable String experimentId) {
        metricService.deleteMetricsByExperiment(experimentId);
        return ResponseEntity.noContent().build();
    }
}
