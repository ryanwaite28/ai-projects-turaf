package com.turaf.experiment.domain;

import com.turaf.common.domain.AggregateRoot;
import com.turaf.common.tenant.TenantAware;
import com.turaf.experiment.domain.event.ExperimentCompleted;
import com.turaf.experiment.domain.event.ExperimentCreated;
import com.turaf.experiment.domain.event.ExperimentStarted;
import com.turaf.experiment.domain.event.ExperimentCancelled;

import java.time.Instant;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public class Experiment extends AggregateRoot<ExperimentId> implements TenantAware {
    private String organizationId;
    private HypothesisId hypothesisId;
    private String name;
    private String description;
    private ExperimentStatus status;
    private Instant startedAt;
    private Instant completedAt;
    private String createdBy;
    private Instant createdAt;
    private Instant updatedAt;

    public Experiment(ExperimentId id, String organizationId, HypothesisId hypothesisId,
                      String name, String description, String createdBy) {
        super(id);
        this.organizationId = Objects.requireNonNull(organizationId, "organizationId cannot be null");
        this.hypothesisId = Objects.requireNonNull(hypothesisId, "hypothesisId cannot be null");
        this.name = validateName(name);
        this.description = description;
        this.status = ExperimentStatus.DRAFT;
        this.createdBy = Objects.requireNonNull(createdBy, "createdBy cannot be null");
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();

        registerEvent(new ExperimentCreated(
            UUID.randomUUID().toString(),
            id.getValue(),
            organizationId,
            hypothesisId.getValue(),
            name,
            description,
            createdBy,
            createdAt
        ));
    }

    public void start() {
        ExperimentStateMachine.validateTransition(status, ExperimentStatus.RUNNING);
        this.status = ExperimentStatus.RUNNING;
        this.startedAt = Instant.now();
        this.updatedAt = Instant.now();

        registerEvent(new ExperimentStarted(
            UUID.randomUUID().toString(),
            getId().getValue(),
            organizationId,
            hypothesisId.getValue(),
            startedAt
        ));
    }

    public void complete() {
        ExperimentStateMachine.validateTransition(status, ExperimentStatus.COMPLETED);
        this.status = ExperimentStatus.COMPLETED;
        this.completedAt = Instant.now();
        this.updatedAt = Instant.now();

        registerEvent(new ExperimentCompleted(
            UUID.randomUUID().toString(),
            getId().getValue(),
            organizationId,
            hypothesisId.getValue(),
            completedAt
        ));
    }

    public void cancel() {
        ExperimentStateMachine.validateTransition(status, ExperimentStatus.CANCELLED);
        this.status = ExperimentStatus.CANCELLED;
        this.updatedAt = Instant.now();

        registerEvent(new ExperimentCancelled(
            UUID.randomUUID().toString(),
            getId().getValue(),
            organizationId,
            hypothesisId.getValue(),
            Instant.now()
        ));
    }

    public Set<ExperimentStatus> getAllowedTransitions() {
        return ExperimentStateMachine.getAllowedTransitions(status);
    }

    public boolean canTransitionTo(ExperimentStatus targetStatus) {
        return ExperimentStateMachine.canTransition(status, targetStatus);
    }

    public void update(String name, String description) {
        if (status != ExperimentStatus.DRAFT) {
            throw new IllegalStateException("Can only update experiments in DRAFT status");
        }
        this.name = validateName(name);
        this.description = description;
        this.updatedAt = Instant.now();
    }

    private String validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Name cannot be null or empty");
        }
        if (name.length() > 200) {
            throw new IllegalArgumentException("Name must be 1-200 characters");
        }
        return name;
    }

    @Override
    public String getOrganizationId() {
        return organizationId;
    }

    @Override
    public void setOrganizationId(String organizationId) {
        this.organizationId = organizationId;
    }

    public HypothesisId getHypothesisId() {
        return hypothesisId;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public ExperimentStatus getStatus() {
        return status;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
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
