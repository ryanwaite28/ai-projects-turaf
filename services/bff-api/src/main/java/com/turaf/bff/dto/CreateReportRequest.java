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
public class CreateReportRequest {
    
    @NotBlank(message = "Type is required")
    private String type;
    
    @NotBlank(message = "Format is required")
    private String format;
    
    private String experimentId;
    private String hypothesisId;
    private String problemId;
    private String organizationId;
}
