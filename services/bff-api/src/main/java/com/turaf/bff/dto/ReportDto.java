package com.turaf.bff.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportDto {
    
    private String id;
    private String type;
    private String format;
    private String status;
    private String experimentId;
    private String hypothesisId;
    private String problemId;
    private String organizationId;
    private String downloadUrl;
    private String createdBy;
    private String createdAt;
    private String updatedAt;
}
