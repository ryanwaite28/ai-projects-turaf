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
public class ExperimentFullDto {
    private ExperimentDto experiment;
    private List<MetricDto> metrics;
    private Integer totalMetrics;
}
