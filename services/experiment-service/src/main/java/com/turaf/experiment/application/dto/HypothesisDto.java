package com.turaf.experiment.application.dto;

import com.turaf.experiment.domain.Hypothesis;

import java.time.Instant;

public class HypothesisDto {
    
    private String id;
    private String organizationId;
    private String problemId;
    private String statement;
    private String expectedOutcome;
    private String createdBy;
    private Instant createdAt;
    private Instant updatedAt;

    public HypothesisDto() {
    }

    public static HypothesisDto fromDomain(Hypothesis hypothesis) {
        HypothesisDto dto = new HypothesisDto();
        dto.id = hypothesis.getId().getValue();
        dto.organizationId = hypothesis.getOrganizationId();
        dto.problemId = hypothesis.getProblemId().getValue();
        dto.statement = hypothesis.getStatement();
        dto.expectedOutcome = hypothesis.getExpectedOutcome();
        dto.createdBy = hypothesis.getCreatedBy();
        dto.createdAt = hypothesis.getCreatedAt();
        dto.updatedAt = hypothesis.getUpdatedAt();
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
