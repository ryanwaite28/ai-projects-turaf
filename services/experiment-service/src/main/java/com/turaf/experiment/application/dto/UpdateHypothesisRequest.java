package com.turaf.experiment.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class UpdateHypothesisRequest {
    
    @NotBlank(message = "Statement is required")
    @Size(min = 1, max = 500, message = "Statement must be between 1 and 500 characters")
    private String statement;
    
    private String expectedOutcome;

    public UpdateHypothesisRequest() {
    }

    public UpdateHypothesisRequest(String statement, String expectedOutcome) {
        this.statement = statement;
        this.expectedOutcome = expectedOutcome;
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
}
