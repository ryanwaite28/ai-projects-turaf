package com.turaf.bff.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HypothesisDto {
    
    private String id;
    private String problemId;
    private String statement;
    private String expectedOutcome;
    private String successCriteria;
    private String organizationId;
    private String createdBy;
    private String createdAt;
    private String updatedAt;
}
