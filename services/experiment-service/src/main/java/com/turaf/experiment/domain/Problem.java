package com.turaf.experiment.domain;

import com.turaf.common.domain.AggregateRoot;
import com.turaf.common.domain.TenantAware;
import com.turaf.experiment.domain.event.ProblemCreated;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public class Problem extends AggregateRoot<ProblemId> implements TenantAware {
    private String organizationId;
    private String title;
    private String description;
    private String createdBy;
    private Instant createdAt;
    private Instant updatedAt;

    public Problem(ProblemId id, String organizationId, String title, String description, String createdBy) {
        super(id);
        this.organizationId = Objects.requireNonNull(organizationId, "organizationId cannot be null");
        this.title = validateTitle(title);
        this.description = description;
        this.createdBy = Objects.requireNonNull(createdBy, "createdBy cannot be null");
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();

        registerEvent(new ProblemCreated(
            UUID.randomUUID().toString(),
            id.getValue(),
            organizationId,
            title,
            description,
            createdBy,
            createdAt
        ));
    }

    public void update(String title, String description) {
        this.title = validateTitle(title);
        this.description = description;
        this.updatedAt = Instant.now();
    }

    private String validateTitle(String title) {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("Title cannot be null or empty");
        }
        if (title.length() > 200) {
            throw new IllegalArgumentException("Title must be 1-200 characters");
        }
        return title;
    }

    @Override
    public String getOrganizationId() {
        return organizationId;
    }

    @Override
    public void setOrganizationId(String organizationId) {
        this.organizationId = organizationId;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
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
