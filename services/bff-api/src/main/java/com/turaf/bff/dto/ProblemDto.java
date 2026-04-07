package com.turaf.bff.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProblemDto {
    
    private String id;
    private String title;
    private String description;
    private String affectedUsers;
    private String context;
    private String organizationId;
    private String createdBy;
    private String createdAt;
    private String updatedAt;
}
