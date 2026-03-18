package com.turaf.experiment.infrastructure.persistence;

import com.turaf.experiment.domain.Problem;
import com.turaf.experiment.domain.ProblemId;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "problems")
public class ProblemJpaEntity {
    
    @Id
    @Column(length = 36)
    private String id;
    
    @Column(name = "organization_id", nullable = false, length = 36)
    private String organizationId;
    
    @Column(nullable = false, length = 200)
    private String title;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    @Column(name = "created_by", nullable = false, length = 36)
    private String createdBy;
    
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected ProblemJpaEntity() {
        // JPA requires default constructor
    }

    public Problem toDomain() {
        Problem problem = new Problem(
            ProblemId.of(id),
            organizationId,
            title,
            description,
            createdBy
        );
        problem.clearDomainEvents();
        return problem;
    }

    public static ProblemJpaEntity fromDomain(Problem problem) {
        ProblemJpaEntity entity = new ProblemJpaEntity();
        entity.id = problem.getId().getValue();
        entity.organizationId = problem.getOrganizationId();
        entity.title = problem.getTitle();
        entity.description = problem.getDescription();
        entity.createdBy = problem.getCreatedBy();
        entity.createdAt = problem.getCreatedAt();
        entity.updatedAt = problem.getUpdatedAt();
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
