package com.turaf.bff.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecordMetricRequest {
    
    @NotBlank(message = "Experiment ID is required")
    private String experimentId;
    
    @NotBlank(message = "Name is required")
    private String name;
    
    @NotNull(message = "Value is required")
    private Double value;
    
    private String unit;
    
    private LocalDateTime recordedAt;
}
