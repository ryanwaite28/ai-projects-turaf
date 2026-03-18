package com.turaf.metrics.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.turaf.common.multitenancy.TenantContextHolder;
import com.turaf.metrics.application.dto.AggregatedMetricsDto;
import com.turaf.metrics.application.dto.BatchRecordRequest;
import com.turaf.metrics.application.dto.MetricDto;
import com.turaf.metrics.application.dto.RecordMetricRequest;
import com.turaf.metrics.domain.MetricRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MetricControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MetricRepository metricRepository;

    @BeforeEach
    void setUp() {
        TenantContextHolder.setOrganizationId("org-integration-test");
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    @WithMockUser
    void shouldRecordMetricEndToEnd() throws Exception {
        // Given
        RecordMetricRequest request = new RecordMetricRequest(
            "exp-integration-001",
            "response_time",
            125.5,
            "GAUGE"
        );

        // When
        MvcResult result = mockMvc.perform(post("/api/v1/metrics")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").exists())
            .andExpect(jsonPath("$.organizationId").value("org-integration-test"))
            .andExpect(jsonPath("$.experimentId").value("exp-integration-001"))
            .andExpect(jsonPath("$.name").value("response_time"))
            .andExpect(jsonPath("$.value").value(125.5))
            .andExpect(jsonPath("$.type").value("GAUGE"))
            .andReturn();

        // Then - Verify metric was persisted
        String responseBody = result.getResponse().getContentAsString();
        MetricDto metricDto = objectMapper.readValue(responseBody, MetricDto.class);
        assertNotNull(metricDto.getId());
        assertNotNull(metricDto.getTimestamp());
    }

    @Test
    @WithMockUser
    void shouldRecordMetricWithTags() throws Exception {
        // Given
        Map<String, String> tags = new HashMap<>();
        tags.put("region", "us-east-1");
        tags.put("environment", "production");

        RecordMetricRequest request = new RecordMetricRequest(
            "exp-integration-002",
            "cpu_usage",
            75.0,
            "GAUGE",
            null,
            tags
        );

        // When & Then
        mockMvc.perform(post("/api/v1/metrics")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.tags.region").value("us-east-1"))
            .andExpect(jsonPath("$.tags.environment").value("production"));
    }

    @Test
    @WithMockUser
    void shouldRecordBatchMetrics() throws Exception {
        // Given
        List<RecordMetricRequest> metrics = Arrays.asList(
            new RecordMetricRequest("exp-integration-003", "metric1", 100.0, "COUNTER"),
            new RecordMetricRequest("exp-integration-003", "metric2", 200.0, "GAUGE"),
            new RecordMetricRequest("exp-integration-003", "metric3", 300.0, "HISTOGRAM")
        );

        BatchRecordRequest request = new BatchRecordRequest("exp-integration-003", metrics);

        // When & Then
        mockMvc.perform(post("/api/v1/metrics/batch")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser
    void shouldGetMetricsWithTimeRange() throws Exception {
        // Given - Record some metrics first
        Instant now = Instant.now();
        Instant start = now.minus(1, ChronoUnit.HOURS);
        Instant end = now.plus(1, ChronoUnit.HOURS);

        RecordMetricRequest request1 = new RecordMetricRequest(
            "exp-integration-004",
            "test_metric",
            100.0,
            "GAUGE",
            now,
            null
        );

        RecordMetricRequest request2 = new RecordMetricRequest(
            "exp-integration-004",
            "test_metric",
            200.0,
            "GAUGE",
            now.plus(10, ChronoUnit.MINUTES),
            null
        );

        mockMvc.perform(post("/api/v1/metrics")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request1)))
            .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/metrics")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request2)))
            .andExpect(status().isCreated());

        // When & Then - Retrieve metrics
        mockMvc.perform(get("/api/v1/metrics")
                .param("experimentId", "exp-integration-004")
                .param("start", start.toString())
                .param("end", end.toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].name").value("test_metric"));
    }

    @Test
    @WithMockUser
    void shouldGetMetricsWithNameFilter() throws Exception {
        // Given - Record metrics with different names
        Instant now = Instant.now();
        Instant start = now.minus(1, ChronoUnit.HOURS);
        Instant end = now.plus(1, ChronoUnit.HOURS);

        mockMvc.perform(post("/api/v1/metrics")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    new RecordMetricRequest("exp-integration-005", "response_time", 100.0, "GAUGE", now, null))))
            .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/metrics")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    new RecordMetricRequest("exp-integration-005", "error_rate", 0.05, "COUNTER", now, null))))
            .andExpect(status().isCreated());

        // When & Then - Filter by name
        mockMvc.perform(get("/api/v1/metrics")
                .param("experimentId", "exp-integration-005")
                .param("name", "response_time")
                .param("start", start.toString())
                .param("end", end.toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].name").value("response_time"));
    }

    @Test
    @WithMockUser
    void shouldAggregateMetrics() throws Exception {
        // Given - Record multiple metrics
        Instant now = Instant.now();
        Instant start = now.minus(1, ChronoUnit.HOURS);
        Instant end = now.plus(1, ChronoUnit.HOURS);

        for (int i = 1; i <= 5; i++) {
            RecordMetricRequest request = new RecordMetricRequest(
                "exp-integration-006",
                "aggregation_test",
                (double) (i * 100),
                "GAUGE",
                now.plus(i, ChronoUnit.MINUTES),
                null
            );

            mockMvc.perform(post("/api/v1/metrics")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
        }

        // When & Then - Aggregate
        MvcResult result = mockMvc.perform(get("/api/v1/metrics/aggregate")
                .param("experimentId", "exp-integration-006")
                .param("name", "aggregation_test")
                .param("start", start.toString())
                .param("end", end.toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.metricName").value("aggregation_test"))
            .andExpect(jsonPath("$.count").value(5))
            .andExpect(jsonPath("$.min").value(100.0))
            .andExpect(jsonPath("$.max").value(500.0))
            .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        AggregatedMetricsDto aggregated = objectMapper.readValue(responseBody, AggregatedMetricsDto.class);
        assertEquals(1500.0, aggregated.getSum(), 0.01);
        assertEquals(300.0, aggregated.getAverage(), 0.01);
    }

    @Test
    @WithMockUser
    void shouldAggregateAllMetrics() throws Exception {
        // Given - Record different metrics
        Instant now = Instant.now();
        Instant start = now.minus(1, ChronoUnit.HOURS);
        Instant end = now.plus(1, ChronoUnit.HOURS);

        mockMvc.perform(post("/api/v1/metrics")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    new RecordMetricRequest("exp-integration-007", "metric_a", 100.0, "GAUGE", now, null))))
            .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/metrics")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    new RecordMetricRequest("exp-integration-007", "metric_a", 200.0, "GAUGE", now, null))))
            .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/metrics")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    new RecordMetricRequest("exp-integration-007", "metric_b", 50.0, "COUNTER", now, null))))
            .andExpect(status().isCreated());

        // When & Then
        mockMvc.perform(get("/api/v1/metrics/aggregate/all")
                .param("experimentId", "exp-integration-007")
                .param("start", start.toString())
                .param("end", end.toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.metric_a").exists())
            .andExpect(jsonPath("$.metric_b").exists())
            .andExpect(jsonPath("$.metric_a.count").value(2))
            .andExpect(jsonPath("$.metric_b.count").value(1));
    }

    @Test
    @WithMockUser
    void shouldDeleteMetricsByExperiment() throws Exception {
        // Given - Record a metric
        RecordMetricRequest request = new RecordMetricRequest(
            "exp-integration-008",
            "delete_test",
            100.0,
            "GAUGE"
        );

        mockMvc.perform(post("/api/v1/metrics")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated());

        // When - Delete
        mockMvc.perform(delete("/api/v1/metrics/experiment/exp-integration-008")
                .with(csrf()))
            .andExpect(status().isNoContent());

        // Then - Verify deleted
        Instant now = Instant.now();
        mockMvc.perform(get("/api/v1/metrics")
                .param("experimentId", "exp-integration-008")
                .param("start", now.minus(1, ChronoUnit.HOURS).toString())
                .param("end", now.plus(1, ChronoUnit.HOURS).toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @WithMockUser
    void shouldReturnBadRequestForInvalidMetricType() throws Exception {
        // Given
        RecordMetricRequest request = new RecordMetricRequest(
            "exp-integration-009",
            "invalid_metric",
            100.0,
            "INVALID_TYPE"
        );

        // When & Then
        mockMvc.perform(post("/api/v1/metrics")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser
    void shouldReturnBadRequestForMissingRequiredFields() throws Exception {
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
    }

    @Test
    void shouldReturnUnauthorizedWhenNotAuthenticated() throws Exception {
        // Given
        RecordMetricRequest request = new RecordMetricRequest(
            "exp-integration-010",
            "unauthorized_test",
            100.0,
            "GAUGE"
        );

        // When & Then
        mockMvc.perform(post("/api/v1/metrics")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isUnauthorized());
    }
}
