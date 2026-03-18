package com.turaf.metrics.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.Map;

public class RecordMetricRequest {

    @NotBlank(message = "Experiment ID is required")
    private String experimentId;

    @NotBlank(message = "Metric name is required")
    @Size(max = 100, message = "Metric name cannot exceed 100 characters")
    private String name;

    @NotNull(message = "Metric value is required")
    private Double value;

    @NotBlank(message = "Metric type is required")
    private String type;

    private Instant timestamp;

    private Map<String, String> tags;

    public RecordMetricRequest() {
    }

    public RecordMetricRequest(String experimentId, String name, Double value, String type) {
        this.experimentId = experimentId;
        this.name = name;
        this.value = value;
        this.type = type;
    }

    public RecordMetricRequest(String experimentId, String name, Double value, String type, 
                              Instant timestamp, Map<String, String> tags) {
        this.experimentId = experimentId;
        this.name = name;
        this.value = value;
        this.type = type;
        this.timestamp = timestamp;
        this.tags = tags;
    }

    public String getExperimentId() {
        return experimentId;
    }

    public void setExperimentId(String experimentId) {
        this.experimentId = experimentId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Double getValue() {
        return value;
    }

    public void setValue(Double value) {
        this.value = value;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public Map<String, String> getTags() {
        return tags;
    }

    public void setTags(Map<String, String> tags) {
        this.tags = tags;
    }
}
