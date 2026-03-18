package com.turaf.metrics.interfaces.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.turaf.metrics.application.AggregationService;
import com.turaf.metrics.application.BatchMetricService;
import com.turaf.metrics.application.MetricService;
import com.turaf.metrics.application.dto.AggregatedMetricsDto;
import com.turaf.metrics.application.dto.BatchRecordRequest;
import com.turaf.metrics.application.dto.MetricDto;
import com.turaf.metrics.application.dto.RecordMetricRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.*;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(MetricController.class)
class MetricControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private MetricService metricService;

    @MockBean
    private AggregationService aggregationService;

    @MockBean
    private BatchMetricService batchMetricService;

    @Test
    @WithMockUser
    void shouldRecordMetric() throws Exception {
        // Given
        RecordMetricRequest request = new RecordMetricRequest(
            "exp-123",
            "response_time",
            125.5,
            "GAUGE"
        );

        MetricDto metricDto = new MetricDto(
            "metric-id",
            "org-123",
            "exp-123",
            "response_time",
            125.5,
            "GAUGE",
            Instant.now(),
            new HashMap<>()
        );

        when(metricService.recordMetric(any(RecordMetricRequest.class))).thenReturn(metricDto);

        // When & Then
        mockMvc.perform(post("/api/v1/metrics")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value("metric-id"))
            .andExpect(jsonPath("$.name").value("response_time"))
            .andExpect(jsonPath("$.value").value(125.5))
            .andExpect(jsonPath("$.type").value("GAUGE"));

        verify(metricService).recordMetric(any(RecordMetricRequest.class));
    }

    @Test
    @WithMockUser
    void shouldReturnBadRequestWhenRecordMetricValidationFails() throws Exception {
        // Given
        RecordMetricRequest request = new RecordMetricRequest();
        request.setExperimentId("");
        request.setName("");
        request.setValue(null);
        request.setType("");

        // When & Then
        mockMvc.perform(post("/api/v1/metrics")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Validation failed"));

        verify(metricService, never()).recordMetric(any());
    }

    @Test
    @WithMockUser
    void shouldRecordBatch() throws Exception {
        // Given
        List<RecordMetricRequest> metrics = Arrays.asList(
            new RecordMetricRequest("exp-123", "metric1", 100.0, "COUNTER"),
            new RecordMetricRequest("exp-123", "metric2", 200.0, "GAUGE")
        );

        BatchRecordRequest request = new BatchRecordRequest("exp-123", metrics);

        doNothing().when(batchMetricService).recordBatch(any(BatchRecordRequest.class));

        // When & Then
        mockMvc.perform(post("/api/v1/metrics/batch")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated());

        verify(batchMetricService).recordBatch(any(BatchRecordRequest.class));
    }

    @Test
    @WithMockUser
    void shouldReturnBadRequestWhenBatchValidationFails() throws Exception {
        // Given
        BatchRecordRequest request = new BatchRecordRequest();
        request.setExperimentId("");
        request.setMetrics(Collections.emptyList());

        // When & Then
        mockMvc.perform(post("/api/v1/metrics/batch")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());

        verify(batchMetricService, never()).recordBatch(any());
    }

    @Test
    @WithMockUser
    void shouldGetMetrics() throws Exception {
        // Given
        String experimentId = "exp-123";
        Instant start = Instant.parse("2024-01-01T00:00:00Z");
        Instant end = Instant.parse("2024-01-02T00:00:00Z");

        List<MetricDto> metrics = Arrays.asList(
            new MetricDto("id1", "org-123", experimentId, "metric1", 100.0, "GAUGE", start, new HashMap<>()),
            new MetricDto("id2", "org-123", experimentId, "metric2", 200.0, "COUNTER", start, new HashMap<>())
        );

        when(metricService.getMetrics(experimentId, null, start, end)).thenReturn(metrics);

        // When & Then
        mockMvc.perform(get("/api/v1/metrics")
                .param("experimentId", experimentId)
                .param("start", start.toString())
                .param("end", end.toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].name").value("metric1"))
            .andExpect(jsonPath("$[1].name").value("metric2"));

        verify(metricService).getMetrics(experimentId, null, start, end);
    }

    @Test
    @WithMockUser
    void shouldGetMetricsWithNameFilter() throws Exception {
        // Given
        String experimentId = "exp-123";
        String name = "response_time";
        Instant start = Instant.parse("2024-01-01T00:00:00Z");
        Instant end = Instant.parse("2024-01-02T00:00:00Z");

        List<MetricDto> metrics = Collections.singletonList(
            new MetricDto("id1", "org-123", experimentId, name, 100.0, "GAUGE", start, new HashMap<>())
        );

        when(metricService.getMetrics(experimentId, name, start, end)).thenReturn(metrics);

        // When & Then
        mockMvc.perform(get("/api/v1/metrics")
                .param("experimentId", experimentId)
                .param("name", name)
                .param("start", start.toString())
                .param("end", end.toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].name").value(name));

        verify(metricService).getMetrics(experimentId, name, start, end);
    }

    @Test
    @WithMockUser
    void shouldAggregateMetrics() throws Exception {
        // Given
        String experimentId = "exp-123";
        String name = "response_time";
        Instant start = Instant.parse("2024-01-01T00:00:00Z");
        Instant end = Instant.parse("2024-01-02T00:00:00Z");

        AggregatedMetricsDto aggregated = new AggregatedMetricsDto(
            name, 10, 1000.0, 100.0, 50.0, 200.0, start, end
        );

        when(aggregationService.aggregateMetrics(experimentId, name, start, end))
            .thenReturn(aggregated);

        // When & Then
        mockMvc.perform(get("/api/v1/metrics/aggregate")
                .param("experimentId", experimentId)
                .param("name", name)
                .param("start", start.toString())
                .param("end", end.toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.metricName").value(name))
            .andExpect(jsonPath("$.count").value(10))
            .andExpect(jsonPath("$.average").value(100.0))
            .andExpect(jsonPath("$.min").value(50.0))
            .andExpect(jsonPath("$.max").value(200.0));

        verify(aggregationService).aggregateMetrics(experimentId, name, start, end);
    }

    @Test
    @WithMockUser
    void shouldAggregateAllMetrics() throws Exception {
        // Given
        String experimentId = "exp-123";
        Instant start = Instant.parse("2024-01-01T00:00:00Z");
        Instant end = Instant.parse("2024-01-02T00:00:00Z");

        Map<String, AggregatedMetricsDto> aggregated = new HashMap<>();
        aggregated.put("metric1", new AggregatedMetricsDto("metric1", 5, 500.0, 100.0, 50.0, 150.0, start, end));
        aggregated.put("metric2", new AggregatedMetricsDto("metric2", 3, 300.0, 100.0, 80.0, 120.0, start, end));

        when(aggregationService.aggregateAllMetrics(experimentId, start, end))
            .thenReturn(aggregated);

        // When & Then
        mockMvc.perform(get("/api/v1/metrics/aggregate/all")
                .param("experimentId", experimentId)
                .param("start", start.toString())
                .param("end", end.toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.metric1.count").value(5))
            .andExpect(jsonPath("$.metric2.count").value(3));

        verify(aggregationService).aggregateAllMetrics(experimentId, start, end);
    }

    @Test
    @WithMockUser
    void shouldDeleteMetricsByExperiment() throws Exception {
        // Given
        String experimentId = "exp-123";

        doNothing().when(metricService).deleteMetricsByExperiment(experimentId);

        // When & Then
        mockMvc.perform(delete("/api/v1/metrics/experiment/{experimentId}", experimentId)
                .with(csrf()))
            .andExpect(status().isNoContent());

        verify(metricService).deleteMetricsByExperiment(experimentId);
    }

    @Test
    void shouldReturnUnauthorizedWhenNotAuthenticated() throws Exception {
        // Given
        RecordMetricRequest request = new RecordMetricRequest(
            "exp-123",
            "metric",
            100.0,
            "GAUGE"
        );

        // When & Then
        mockMvc.perform(post("/api/v1/metrics")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isUnauthorized());

        verify(metricService, never()).recordMetric(any());
    }

    @Test
    @WithMockUser
    void shouldHandleIllegalArgumentException() throws Exception {
        // Given
        RecordMetricRequest request = new RecordMetricRequest(
            "exp-123",
            "metric",
            100.0,
            "INVALID_TYPE"
        );

        when(metricService.recordMetric(any(RecordMetricRequest.class)))
            .thenThrow(new IllegalArgumentException("Invalid metric type"));

        // When & Then
        mockMvc.perform(post("/api/v1/metrics")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Invalid metric type"));
    }

    @Test
    @WithMockUser
    void shouldReturnEmptyListWhenNoMetricsFound() throws Exception {
        // Given
        String experimentId = "exp-123";
        Instant start = Instant.parse("2024-01-01T00:00:00Z");
        Instant end = Instant.parse("2024-01-02T00:00:00Z");

        when(metricService.getMetrics(experimentId, null, start, end))
            .thenReturn(Collections.emptyList());

        // When & Then
        mockMvc.perform(get("/api/v1/metrics")
                .param("experimentId", experimentId)
                .param("start", start.toString())
                .param("end", end.toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @WithMockUser
    void shouldReturnEmptyMapWhenNoAggregationsFound() throws Exception {
        // Given
        String experimentId = "exp-123";
        Instant start = Instant.parse("2024-01-01T00:00:00Z");
        Instant end = Instant.parse("2024-01-02T00:00:00Z");

        when(aggregationService.aggregateAllMetrics(experimentId, start, end))
            .thenReturn(Collections.emptyMap());

        // When & Then
        mockMvc.perform(get("/api/v1/metrics/aggregate/all")
                .param("experimentId", experimentId)
                .param("start", start.toString())
                .param("end", end.toString()))
            .andExpect(status().isOk())
            .andExpect(content().string("{}"));
    }
}
