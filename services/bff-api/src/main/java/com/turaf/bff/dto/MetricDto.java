package com.turaf.bff.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MetricDto {
    private String id;
    private String experimentId;
    private String name;
    private Double value;
    private String unit;
    private LocalDateTime recordedAt;
    private LocalDateTime createdAt;
}
