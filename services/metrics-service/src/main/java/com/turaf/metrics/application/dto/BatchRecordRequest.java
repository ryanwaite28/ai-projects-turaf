package com.turaf.metrics.application.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public class BatchRecordRequest {

    @NotBlank(message = "Experiment ID is required")
    private String experimentId;

    @NotEmpty(message = "Metrics list cannot be empty")
    @Valid
    private List<RecordMetricRequest> metrics;

    public BatchRecordRequest() {
    }

    public BatchRecordRequest(String experimentId, List<RecordMetricRequest> metrics) {
        this.experimentId = experimentId;
        this.metrics = metrics;
    }

    public String getExperimentId() {
        return experimentId;
    }

    public void setExperimentId(String experimentId) {
        this.experimentId = experimentId;
    }

    public List<RecordMetricRequest> getMetrics() {
        return metrics;
    }

    public void setMetrics(List<RecordMetricRequest> metrics) {
        this.metrics = metrics;
    }
}
