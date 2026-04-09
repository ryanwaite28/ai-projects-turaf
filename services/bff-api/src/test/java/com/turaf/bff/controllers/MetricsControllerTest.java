package com.turaf.bff.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.turaf.bff.clients.MetricsServiceClient;
import com.turaf.bff.dto.MetricDto;
import com.turaf.bff.dto.RecordMetricRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(MetricsController.class)
class MetricsControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @MockBean
    private MetricsServiceClient metricsServiceClient;
    
    @Test
    @WithMockUser(username = "user-123", authorities = {"ROLE_USER"})
    void testRecordMetric_Success() throws Exception {
        RecordMetricRequest request = RecordMetricRequest.builder()
            .experimentId("exp-123")
            .name("conversion_rate")
            .value(0.25)
            .unit("percentage")
            .build();
        
        MetricDto metric = MetricDto.builder()
            .id("metric-123")
            .experimentId("exp-123")
            .name("conversion_rate")
            .value(0.25)
            .build();
        
        when(metricsServiceClient.recordMetric(any(), anyString(), anyString()))
            .thenReturn(metric);
        
        mockMvc.perform(post("/api/v1/metrics")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value("metric-123"))
            .andExpect(jsonPath("$.name").value("conversion_rate"));
    }
    
    @Test
    @WithMockUser(username = "user-123", authorities = {"ROLE_USER"})
    void testGetExperimentMetrics_Success() throws Exception {
        MetricDto metric1 = MetricDto.builder()
            .id("metric-1")
            .experimentId("exp-456")
            .name("clicks")
            .value(100.0)
            .build();
        
        MetricDto metric2 = MetricDto.builder()
            .id("metric-2")
            .experimentId("exp-456")
            .name("views")
            .value(1000.0)
            .build();
        
        when(metricsServiceClient.getExperimentMetrics(anyString(), anyString(), anyString()))
            .thenReturn(List.of(metric1, metric2));
        
        mockMvc.perform(get("/api/v1/metrics/experiments/exp-456"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$[0].id").value("metric-1"))
            .andExpect(jsonPath("$[1].id").value("metric-2"));
    }
    
    @Test
    @WithMockUser(username = "user-123", authorities = {"ROLE_USER"})
    void testGetMetric_Success() throws Exception {
        MetricDto metric = MetricDto.builder()
            .id("metric-789")
            .experimentId("exp-789")
            .name("revenue")
            .value(5000.0)
            .build();
        
        when(metricsServiceClient.getMetric(anyString(), anyString(), anyString()))
            .thenReturn(metric);
        
        mockMvc.perform(get("/api/v1/metrics/metric-789"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value("metric-789"))
            .andExpect(jsonPath("$.name").value("revenue"));
    }
    
    @Test
    @WithMockUser(username = "user-123", authorities = {"ROLE_USER"})
    void testDeleteMetric_Success() throws Exception {
        doNothing().when(metricsServiceClient).deleteMetric(anyString(), anyString(), anyString());
        
        mockMvc.perform(delete("/api/v1/metrics/metric-999"))
            .andExpect(status().isOk());
    }
}
