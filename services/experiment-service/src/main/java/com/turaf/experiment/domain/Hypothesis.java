package com.turaf.experiment.domain;

import com.turaf.common.domain.AggregateRoot;
import com.turaf.common.domain.TenantAware;
import com.turaf.experiment.domain.event.HypothesisCreated;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public class Hypothesis extends AggregateRoot<HypothesisId> implements TenantAware {
    private String organizationId;
    private ProblemId problemId;
    private String statement;
    private String expectedOutcome;
    private String createdBy;
    private Instant createdAt;
    private Instant updatedAt;

    public Hypothesis(HypothesisId id, String organizationId, ProblemId problemId,
                      String statement, String expectedOutcome, String createdBy) {
        super(id);
        this.organizationId = Objects.requireNonNull(organizationId, "organizationId cannot be null");
        this.problemId = Objects.requireNonNull(problemId, "problemId cannot be null");
        this.statement = validateStatement(statement);
        this.expectedOutcome = expectedOutcome;
        this.createdBy = Objects.requireNonNull(createdBy, "createdBy cannot be null");
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();

        registerEvent(new HypothesisCreated(
            UUID.randomUUID().toString(),
            id.getValue(),
            organizationId,
            problemId.getValue(),
            statement,
            expectedOutcome,
            createdBy,
            createdAt
        ));
    }

    public void update(String statement, String expectedOutcome) {
        this.statement = validateStatement(statement);
        this.expectedOutcome = expectedOutcome;
        this.updatedAt = Instant.now();
    }

    private String validateStatement(String statement) {
        if (statement == null || statement.isBlank()) {
            throw new IllegalArgumentException("Statement cannot be null or empty");
        }
        if (statement.length() > 500) {
            throw new IllegalArgumentException("Statement must be 1-500 characters");
        }
        return statement;
    }

    @Override
    public String getOrganizationId() {
        return organizationId;
    }

    @Override
    public void setOrganizationId(String organizationId) {
        this.organizationId = organizationId;
    }

    public ProblemId getProblemId() {
        return problemId;
    }

    public String getStatement() {
        return statement;
    }

    public String getExpectedOutcome() {
        return expectedOutcome;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
