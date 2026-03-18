package com.turaf.experiment.infrastructure.persistence;

import com.turaf.experiment.domain.Experiment;
import com.turaf.experiment.domain.ExperimentId;
import com.turaf.experiment.domain.ExperimentStatus;
import com.turaf.experiment.domain.HypothesisId;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "experiments")
public class ExperimentJpaEntity {
    
    @Id
    @Column(length = 36)
    private String id;
    
    @Column(name = "organization_id", nullable = false, length = 36)
    private String organizationId;
    
    @Column(name = "hypothesis_id", nullable = false, length = 36)
    private String hypothesisId;
    
    @Column(nullable = false, length = 200)
    private String name;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ExperimentStatus status;
    
    @Column(name = "started_at")
    private Instant startedAt;
    
    @Column(name = "completed_at")
    private Instant completedAt;
    
    @Column(name = "created_by", nullable = false, length = 36)
    private String createdBy;
    
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected ExperimentJpaEntity() {
        // JPA requires default constructor
    }

    public Experiment toDomain() {
        Experiment experiment = new Experiment(
            ExperimentId.of(id),
            organizationId,
            HypothesisId.of(hypothesisId),
            name,
            description,
            createdBy
        );
        experiment.clearDomainEvents();
        return experiment;
    }

    public static ExperimentJpaEntity fromDomain(Experiment experiment) {
        ExperimentJpaEntity entity = new ExperimentJpaEntity();
        entity.id = experiment.getId().getValue();
        entity.organizationId = experiment.getOrganizationId();
        entity.hypothesisId = experiment.getHypothesisId().getValue();
        entity.name = experiment.getName();
        entity.description = experiment.getDescription();
        entity.status = experiment.getStatus();
        entity.startedAt = experiment.getStartedAt();
        entity.completedAt = experiment.getCompletedAt();
        entity.createdBy = experiment.getCreatedBy();
        entity.createdAt = experiment.getCreatedAt();
        entity.updatedAt = experiment.getUpdatedAt();
        return entity;
    }

    // Getters and setters
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

    public String getHypothesisId() {
        return hypothesisId;
    }

    public void setHypothesisId(String hypothesisId) {
        this.hypothesisId = hypothesisId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public ExperimentStatus getStatus() {
        return status;
    }

    public void setStatus(ExperimentStatus status) {
        this.status = status;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Instant startedAt) {
        this.startedAt = startedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
