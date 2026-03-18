package com.turaf.experiment.infrastructure.persistence;

import com.turaf.experiment.domain.Hypothesis;
import com.turaf.experiment.domain.HypothesisId;
import com.turaf.experiment.domain.ProblemId;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "hypotheses")
public class HypothesisJpaEntity {
    
    @Id
    @Column(length = 36)
    private String id;
    
    @Column(name = "organization_id", nullable = false, length = 36)
    private String organizationId;
    
    @Column(name = "problem_id", nullable = false, length = 36)
    private String problemId;
    
    @Column(nullable = false, length = 500)
    private String statement;
    
    @Column(name = "expected_outcome", columnDefinition = "TEXT")
    private String expectedOutcome;
    
    @Column(name = "created_by", nullable = false, length = 36)
    private String createdBy;
    
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected HypothesisJpaEntity() {
        // JPA requires default constructor
    }

    public Hypothesis toDomain() {
        Hypothesis hypothesis = new Hypothesis(
            HypothesisId.of(id),
            organizationId,
            ProblemId.of(problemId),
            statement,
            expectedOutcome,
            createdBy
        );
        hypothesis.clearDomainEvents();
        return hypothesis;
    }

    public static HypothesisJpaEntity fromDomain(Hypothesis hypothesis) {
        HypothesisJpaEntity entity = new HypothesisJpaEntity();
        entity.id = hypothesis.getId().getValue();
        entity.organizationId = hypothesis.getOrganizationId();
        entity.problemId = hypothesis.getProblemId().getValue();
        entity.statement = hypothesis.getStatement();
        entity.expectedOutcome = hypothesis.getExpectedOutcome();
        entity.createdBy = hypothesis.getCreatedBy();
        entity.createdAt = hypothesis.getCreatedAt();
        entity.updatedAt = hypothesis.getUpdatedAt();
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

    public String getProblemId() {
        return problemId;
    }

    public void setProblemId(String problemId) {
        this.problemId = problemId;
    }

    public String getStatement() {
        return statement;
    }

    public void setStatement(String statement) {
        this.statement = statement;
    }

    public String getExpectedOutcome() {
        return expectedOutcome;
    }

    public void setExpectedOutcome(String expectedOutcome) {
        this.expectedOutcome = expectedOutcome;
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
