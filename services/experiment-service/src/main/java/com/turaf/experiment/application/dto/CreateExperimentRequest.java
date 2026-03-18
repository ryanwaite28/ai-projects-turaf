package com.turaf.experiment.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CreateExperimentRequest {
    
    @NotBlank(message = "Hypothesis ID is required")
    private String hypothesisId;
    
    @NotBlank(message = "Name is required")
    @Size(min = 1, max = 200, message = "Name must be between 1 and 200 characters")
    private String name;
    
    private String description;

    public CreateExperimentRequest() {
    }

    public CreateExperimentRequest(String hypothesisId, String name, String description) {
        this.hypothesisId = hypothesisId;
        this.name = name;
        this.description = description;
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
}
