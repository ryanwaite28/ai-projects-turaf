package com.turaf.metrics.infrastructure.persistence;

import com.turaf.metrics.domain.Metric;
import com.turaf.metrics.domain.MetricId;
import com.turaf.metrics.domain.MetricType;
import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import org.hibernate.annotations.Type;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Entity
@Table(name = "metrics")
public class MetricJpaEntity {

    @Id
    private String id;

    @Column(name = "organization_id", nullable = false, length = 36)
    private String organizationId;

    @Column(name = "experiment_id", nullable = false, length = 36)
    private String experimentId;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "value", nullable = false)
    private Double value;

    @Column(name = "type", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private MetricType type;

    @Column(name = "timestamp", nullable = false)
    private Instant timestamp;

    @Type(JsonType.class)
    @Column(name = "tags", columnDefinition = "jsonb")
    private Map<String, String> tags;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public MetricJpaEntity() {
        this.tags = new HashMap<>();
        this.createdAt = Instant.now();
    }

    public Metric toDomain() {
        return new Metric(
            MetricId.of(id),
            organizationId,
            experimentId,
            name,
            value,
            type,
            timestamp,
            tags
        );
    }

    public static MetricJpaEntity fromDomain(Metric metric) {
        MetricJpaEntity entity = new MetricJpaEntity();
        entity.setId(metric.getId().getValue());
        entity.setOrganizationId(metric.getOrganizationId());
        entity.setExperimentId(metric.getExperimentId());
        entity.setName(metric.getName());
        entity.setValue(metric.getValue());
        entity.setType(metric.getType());
        entity.setTimestamp(metric.getTimestamp());
        entity.setTags(new HashMap<>(metric.getTags()));
        return entity;
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

    public MetricType getType() {
        return type;
    }

    public void setType(MetricType type) {
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

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
