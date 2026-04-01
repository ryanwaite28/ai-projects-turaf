package com.turaf.bff.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateHypothesisRequest {
    
    @NotBlank(message = "Problem ID is required")
    private String problemId;
    
    @NotBlank(message = "Statement is required")
    private String statement;
    
    private String expectedOutcome;
    private String successCriteria;
    private String organizationId;
}
