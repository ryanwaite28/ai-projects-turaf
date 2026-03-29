package com.turaf.metrics.domain;

import com.turaf.common.domain.AggregateRoot;
import com.turaf.common.tenant.TenantAware;
import com.turaf.metrics.domain.event.MetricRecorded;

import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class Metric extends AggregateRoot<MetricId> implements TenantAware {

    private String organizationId;
    private final String experimentId;
    private final String name;
    private final Double value;
    private final MetricType type;
    private final Instant timestamp;
    private final Map<String, String> tags;

    public Metric(MetricId id, String organizationId, String experimentId,
                 String name, Double value, MetricType type, Instant timestamp) {
        super(id);
        this.organizationId = validateOrganizationId(organizationId);
        this.experimentId = validateExperimentId(experimentId);
        this.name = validateName(name);
        this.value = validateValue(value);
        this.type = Objects.requireNonNull(type, "MetricType cannot be null");
        this.timestamp = Objects.requireNonNull(timestamp, "Timestamp cannot be null");
        this.tags = new HashMap<>();
        
        registerEvent(new MetricRecorded(
            UUID.randomUUID().toString(),
            organizationId,
            experimentId,
            id.getValue(),
            name,
            value,
            type.name(),
            timestamp
        ));
    }

    public Metric(MetricId id, String organizationId, String experimentId,
                 String name, Double value, MetricType type, Instant timestamp,
                 Map<String, String> tags) {
        super(id);
        this.organizationId = validateOrganizationId(organizationId);
        this.experimentId = validateExperimentId(experimentId);
        this.name = validateName(name);
        this.value = validateValue(value);
        this.type = Objects.requireNonNull(type, "MetricType cannot be null");
        this.timestamp = Objects.requireNonNull(timestamp, "Timestamp cannot be null");
        this.tags = new HashMap<>(tags != null ? tags : Collections.emptyMap());
        
        registerEvent(new MetricRecorded(
            UUID.randomUUID().toString(),
            organizationId,
            experimentId,
            id.getValue(),
            name,
            value,
            type.name(),
            timestamp
        ));
    }

    public void addTag(String key, String value) {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("Tag key cannot be null or blank");
        }
        if (value == null) {
            throw new IllegalArgumentException("Tag value cannot be null");
        }
        this.tags.put(key, value);
    }

    public void removeTag(String key) {
        this.tags.remove(key);
    }

    private String validateOrganizationId(String organizationId) {
        if (organizationId == null || organizationId.isBlank()) {
            throw new IllegalArgumentException("OrganizationId cannot be null or blank");
        }
        return organizationId;
    }

    private String validateExperimentId(String experimentId) {
        if (experimentId == null || experimentId.isBlank()) {
            throw new IllegalArgumentException("ExperimentId cannot be null or blank");
        }
        return experimentId;
    }

    private String validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Metric name cannot be null or blank");
        }
        if (name.length() > 100) {
            throw new IllegalArgumentException("Metric name cannot exceed 100 characters");
        }
        return name;
    }

    private Double validateValue(Double value) {
        if (value == null) {
            throw new IllegalArgumentException("Metric value cannot be null");
        }
        if (value.isNaN() || value.isInfinite()) {
            throw new IllegalArgumentException("Metric value must be a valid number");
        }
        return value;
    }

    @Override
    public String getOrganizationId() {
        return organizationId;
    }
    
    @Override
    public void setOrganizationId(String organizationId) {
        this.organizationId = organizationId;
    }

    public String getExperimentId() {
        return experimentId;
    }

    public String getName() {
        return name;
    }

    public Double getValue() {
        return value;
    }

    public MetricType getType() {
        return type;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public Map<String, String> getTags() {
        return Collections.unmodifiableMap(tags);
    }

    public boolean hasTag(String key) {
        return tags.containsKey(key);
    }

    public String getTag(String key) {
        return tags.get(key);
    }
}
