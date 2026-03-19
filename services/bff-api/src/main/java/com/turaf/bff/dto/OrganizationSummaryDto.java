package com.turaf.bff.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrganizationSummaryDto {
    private OrganizationDto organization;
    private List<MemberDto> members;
    private List<ExperimentDto> experiments;
    private Integer totalMembers;
    private Integer totalExperiments;
}
