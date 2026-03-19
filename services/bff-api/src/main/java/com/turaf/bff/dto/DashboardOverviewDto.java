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
public class DashboardOverviewDto {
    private UserDto user;
    private List<OrganizationDto> organizations;
    private List<ExperimentDto> activeExperiments;
    private Integer totalOrganizations;
    private Integer totalActiveExperiments;
}
