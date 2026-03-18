package com.turaf.metrics.application.dto;

import com.turaf.metrics.domain.Metric;

import java.time.Instant;
import java.util.Map;

public class MetricDto {

    private String id;
    private String organizationId;
    private String experimentId;
    private String name;
    private Double value;
    private String type;
    private Instant timestamp;
    private Map<String, String> tags;

    public MetricDto() {
    }

    public MetricDto(String id, String organizationId, String experimentId, String name,
                    Double value, String type, Instant timestamp, Map<String, String> tags) {
        this.id = id;
        this.organizationId = organizationId;
        this.experimentId = experimentId;
        this.name = name;
        this.value = value;
        this.type = type;
        this.timestamp = timestamp;
        this.tags = tags;
    }

    public static MetricDto fromDomain(Metric metric) {
        return new MetricDto(
            metric.getId().getValue(),
            metric.getOrganizationId(),
            metric.getExperimentId(),
            metric.getName(),
            metric.getValue(),
            metric.getType().name(),
            metric.getTimestamp(),
            metric.getTags()
        );
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getOrganizationId() {
        return organizationId;
    }

    public void setOrganizationId(String organizationId) {
        this.organizationId = organizationId;
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
