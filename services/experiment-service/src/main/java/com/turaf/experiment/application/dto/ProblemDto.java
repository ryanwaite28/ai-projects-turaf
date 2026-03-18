package com.turaf.experiment.application.dto;

import com.turaf.experiment.domain.Problem;

import java.time.Instant;

public class ProblemDto {
    
    private String id;
    private String organizationId;
    private String title;
    private String description;
    private String createdBy;
    private Instant createdAt;
    private Instant updatedAt;

    public ProblemDto() {
    }

    public static ProblemDto fromDomain(Problem problem) {
        ProblemDto dto = new ProblemDto();
        dto.id = problem.getId().getValue();
        dto.organizationId = problem.getOrganizationId();
        dto.title = problem.getTitle();
        dto.description = problem.getDescription();
        dto.createdBy = problem.getCreatedBy();
        dto.createdAt = problem.getCreatedAt();
        dto.updatedAt = problem.getUpdatedAt();
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

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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
