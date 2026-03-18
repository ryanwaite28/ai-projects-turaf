package com.turaf.experiment.application.dto;

import com.turaf.experiment.domain.Experiment;

import java.time.Instant;
import java.util.Set;
import java.util.stream.Collectors;

public class ExperimentDto {
    
    private String id;
    private String organizationId;
    private String hypothesisId;
    private String name;
    private String description;
    private String status;
    private Instant startedAt;
    private Instant completedAt;
    private String createdBy;
    private Instant createdAt;
    private Instant updatedAt;
    private Set<String> allowedTransitions;

    public ExperimentDto() {
    }

    public static ExperimentDto fromDomain(Experiment experiment) {
        ExperimentDto dto = new ExperimentDto();
        dto.id = experiment.getId().getValue();
        dto.organizationId = experiment.getOrganizationId();
        dto.hypothesisId = experiment.getHypothesisId().getValue();
        dto.name = experiment.getName();
        dto.description = experiment.getDescription();
        dto.status = experiment.getStatus().name();
        dto.startedAt = experiment.getStartedAt();
        dto.completedAt = experiment.getCompletedAt();
        dto.createdBy = experiment.getCreatedBy();
        dto.createdAt = experiment.getCreatedAt();
        dto.updatedAt = experiment.getUpdatedAt();
        dto.allowedTransitions = experiment.getAllowedTransitions()
            .stream()
            .map(Enum::name)
            .collect(Collectors.toSet());
        return dto;
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
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

    public Set<String> getAllowedTransitions() {
        return allowedTransitions;
    }

    public void setAllowedTransitions(Set<String> allowedTransitions) {
        this.allowedTransitions = allowedTransitions;
    }
}
