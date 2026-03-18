package com.turaf.metrics.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.turaf.common.multitenancy.TenantContextHolder;
import com.turaf.metrics.application.dto.AggregatedMetricsDto;
import com.turaf.metrics.application.dto.BatchRecordRequest;
import com.turaf.metrics.application.dto.MetricDto;
import com.turaf.metrics.application.dto.RecordMetricRequest;
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
import java.util.ArrayList;
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
class MetricsFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        TenantContextHolder.setOrganizationId("org-flow-test");
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    @WithMockUser
    void shouldCompleteFullMetricsLifecycle() throws Exception {
        String experimentId = "exp-lifecycle-001";
        Instant now = Instant.now();
        Instant start = now.minus(1, ChronoUnit.HOURS);
        Instant end = now.plus(1, ChronoUnit.HOURS);

        // Step 1: Record a single metric
        RecordMetricRequest singleRequest = new RecordMetricRequest(
            experimentId,
            "response_time",
            125.5,
            "GAUGE",
            now,
            null
        );

        MvcResult recordResult = mockMvc.perform(post("/api/v1/metrics")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(singleRequest)))
            .andExpect(status().isCreated())
            .andReturn();

        MetricDto recordedMetric = objectMapper.readValue(
            recordResult.getResponse().getContentAsString(),
            MetricDto.class
        );
        assertNotNull(recordedMetric.getId());

        // Step 2: Record batch metrics
        List<RecordMetricRequest> batchMetrics = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            batchMetrics.add(new RecordMetricRequest(
                experimentId,
                "response_time",
                100.0 + (i * 10),
                "GAUGE",
                now.plus(i, ChronoUnit.MINUTES),
                null
            ));
        }

        BatchRecordRequest batchRequest = new BatchRecordRequest(experimentId, batchMetrics);

        mockMvc.perform(post("/api/v1/metrics/batch")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(batchRequest)))
            .andExpect(status().isCreated());

        // Step 3: Retrieve all metrics
        MvcResult retrieveResult = mockMvc.perform(get("/api/v1/metrics")
                .param("experimentId", experimentId)
                .param("name", "response_time")
                .param("start", start.toString())
                .param("end", end.toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(11))
            .andReturn();

        // Step 4: Aggregate metrics
        MvcResult aggregateResult = mockMvc.perform(get("/api/v1/metrics/aggregate")
                .param("experimentId", experimentId)
                .param("name", "response_time")
                .param("start", start.toString())
                .param("end", end.toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.count").value(11))
            .andReturn();

        AggregatedMetricsDto aggregated = objectMapper.readValue(
            aggregateResult.getResponse().getContentAsString(),
            AggregatedMetricsDto.class
        );
        assertEquals(11, aggregated.getCount());
        assertTrue(aggregated.getAverage() > 0);

        // Step 5: Delete metrics
        mockMvc.perform(delete("/api/v1/metrics/experiment/" + experimentId)
                .with(csrf()))
            .andExpect(status().isNoContent());

        // Step 6: Verify deletion
        mockMvc.perform(get("/api/v1/metrics")
                .param("experimentId", experimentId)
                .param("start", start.toString())
                .param("end", end.toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @WithMockUser
    void shouldHandleMultipleExperimentsIndependently() throws Exception {
        Instant now = Instant.now();
        Instant start = now.minus(1, ChronoUnit.HOURS);
        Instant end = now.plus(1, ChronoUnit.HOURS);

        // Record metrics for experiment 1
        mockMvc.perform(post("/api/v1/metrics")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    new RecordMetricRequest("exp-multi-001", "metric1", 100.0, "GAUGE", now, null))))
            .andExpect(status().isCreated());

        // Record metrics for experiment 2
        mockMvc.perform(post("/api/v1/metrics")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    new RecordMetricRequest("exp-multi-002", "metric1", 200.0, "GAUGE", now, null))))
            .andExpect(status().isCreated());

        // Verify experiment 1 metrics
        mockMvc.perform(get("/api/v1/metrics")
                .param("experimentId", "exp-multi-001")
                .param("start", start.toString())
                .param("end", end.toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].experimentId").value("exp-multi-001"));

        // Verify experiment 2 metrics
        mockMvc.perform(get("/api/v1/metrics")
                .param("experimentId", "exp-multi-002")
                .param("start", start.toString())
                .param("end", end.toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].experimentId").value("exp-multi-002"));
    }

    @Test
    @WithMockUser
    void shouldHandleTimeSeriesDataCorrectly() throws Exception {
        String experimentId = "exp-timeseries-001";
        Instant baseTime = Instant.parse("2024-01-01T12:00:00Z");

        // Record metrics at different times
        for (int hour = 0; hour < 24; hour++) {
            Instant timestamp = baseTime.plus(hour, ChronoUnit.HOURS);
            RecordMetricRequest request = new RecordMetricRequest(
                experimentId,
                "hourly_metric",
                (double) (hour * 10),
                "GAUGE",
                timestamp,
                null
            );

            mockMvc.perform(post("/api/v1/metrics")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
        }

        // Query first 12 hours
        Instant start1 = baseTime;
        Instant end1 = baseTime.plus(12, ChronoUnit.HOURS);

        mockMvc.perform(get("/api/v1/metrics")
                .param("experimentId", experimentId)
                .param("name", "hourly_metric")
                .param("start", start1.toString())
                .param("end", end1.toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(12));

        // Query last 12 hours
        Instant start2 = baseTime.plus(12, ChronoUnit.HOURS);
        Instant end2 = baseTime.plus(24, ChronoUnit.HOURS);

        mockMvc.perform(get("/api/v1/metrics")
                .param("experimentId", experimentId)
                .param("name", "hourly_metric")
                .param("start", start2.toString())
                .param("end", end2.toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(12));
    }

    @Test
    @WithMockUser
    void shouldHandleMetricsWithTags() throws Exception {
        String experimentId = "exp-tags-001";
        Instant now = Instant.now();
        Instant start = now.minus(1, ChronoUnit.HOURS);
        Instant end = now.plus(1, ChronoUnit.HOURS);

        // Record metrics with different tags
        Map<String, String> tags1 = new HashMap<>();
        tags1.put("region", "us-east-1");
        tags1.put("environment", "production");

        Map<String, String> tags2 = new HashMap<>();
        tags2.put("region", "us-west-2");
        tags2.put("environment", "staging");

        mockMvc.perform(post("/api/v1/metrics")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    new RecordMetricRequest(experimentId, "tagged_metric", 100.0, "GAUGE", now, tags1))))
            .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/metrics")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    new RecordMetricRequest(experimentId, "tagged_metric", 200.0, "GAUGE", now, tags2))))
            .andExpect(status().isCreated());

        // Retrieve and verify tags
        MvcResult result = mockMvc.perform(get("/api/v1/metrics")
                .param("experimentId", experimentId)
                .param("name", "tagged_metric")
                .param("start", start.toString())
                .param("end", end.toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2))
            .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        MetricDto[] metrics = objectMapper.readValue(responseBody, MetricDto[].class);

        assertTrue(metrics[0].getTags().containsKey("region"));
        assertTrue(metrics[0].getTags().containsKey("environment"));
    }

    @Test
    @WithMockUser
    void shouldHandleLargeBatchProcessing() throws Exception {
        String experimentId = "exp-large-batch-001";

        // Create a large batch of metrics
        List<RecordMetricRequest> largeB atch = new ArrayList<>();
        for (int i = 0; i < 2500; i++) {
            largeB atch.add(new RecordMetricRequest(
                experimentId,
                "batch_metric_" + (i % 10),
                (double) i,
                "COUNTER",
                Instant.now().plus(i, ChronoUnit.SECONDS),
                null
            ));
        }

        BatchRecordRequest request = new BatchRecordRequest(experimentId, largeB atch);

        // Record large batch
        mockMvc.perform(post("/api/v1/metrics/batch")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated());

        // Verify metrics were recorded
        Instant now = Instant.now();
        mockMvc.perform(get("/api/v1/metrics")
                .param("experimentId", experimentId)
                .param("start", now.minus(1, ChronoUnit.HOURS).toString())
                .param("end", now.plus(1, ChronoUnit.HOURS).toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2500));
    }

    @Test
    @WithMockUser
    void shouldAggregateMultipleMetricTypes() throws Exception {
        String experimentId = "exp-multi-type-001";
        Instant now = Instant.now();
        Instant start = now.minus(1, ChronoUnit.HOURS);
        Instant end = now.plus(1, ChronoUnit.HOURS);

        // Record different metric types
        mockMvc.perform(post("/api/v1/metrics")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    new RecordMetricRequest(experimentId, "counter_metric", 1.0, "COUNTER", now, null))))
            .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/metrics")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    new RecordMetricRequest(experimentId, "gauge_metric", 75.0, "GAUGE", now, null))))
            .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/metrics")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    new RecordMetricRequest(experimentId, "histogram_metric", 125.5, "HISTOGRAM", now, null))))
            .andExpect(status().isCreated());

        // Aggregate all metrics
        mockMvc.perform(get("/api/v1/metrics/aggregate/all")
                .param("experimentId", experimentId)
                .param("start", start.toString())
                .param("end", end.toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.counter_metric").exists())
            .andExpect(jsonPath("$.gauge_metric").exists())
            .andExpect(jsonPath("$.histogram_metric").exists());
    }

    @Test
    @WithMockUser
    void shouldHandleEmptyTimeRanges() throws Exception {
        String experimentId = "exp-empty-range-001";
        Instant futureStart = Instant.now().plus(10, ChronoUnit.DAYS);
        Instant futureEnd = Instant.now().plus(11, ChronoUnit.DAYS);

        // Query future time range (no metrics)
        mockMvc.perform(get("/api/v1/metrics")
                .param("experimentId", experimentId)
                .param("start", futureStart.toString())
                .param("end", futureEnd.toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(0));

        // Aggregate future time range
        mockMvc.perform(get("/api/v1/metrics/aggregate")
                .param("experimentId", experimentId)
                .param("name", "nonexistent_metric")
                .param("start", futureStart.toString())
                .param("end", futureEnd.toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.count").value(0));
    }

    @Test
    @WithMockUser
    void shouldMaintainDataIntegrityAcrossOperations() throws Exception {
        String experimentId = "exp-integrity-001";
        Instant now = Instant.now();
        Instant start = now.minus(1, ChronoUnit.HOURS);
        Instant end = now.plus(1, ChronoUnit.HOURS);

        // Record initial metric
        RecordMetricRequest request = new RecordMetricRequest(
            experimentId,
            "integrity_test",
            100.0,
            "GAUGE",
            now,
            null
        );

        MvcResult recordResult = mockMvc.perform(post("/api/v1/metrics")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andReturn();

        MetricDto recorded = objectMapper.readValue(
            recordResult.getResponse().getContentAsString(),
            MetricDto.class
        );

        // Retrieve and verify same data
        MvcResult retrieveResult = mockMvc.perform(get("/api/v1/metrics")
                .param("experimentId", experimentId)
                .param("name", "integrity_test")
                .param("start", start.toString())
                .param("end", end.toString()))
            .andExpect(status().isOk())
            .andReturn();

        MetricDto[] retrieved = objectMapper.readValue(
            retrieveResult.getResponse().getContentAsString(),
            MetricDto[].class
        );

        assertEquals(1, retrieved.length);
        assertEquals(recorded.getId(), retrieved[0].getId());
        assertEquals(recorded.getValue(), retrieved[0].getValue());
        assertEquals(recorded.getName(), retrieved[0].getName());
        assertEquals(recorded.getType(), retrieved[0].getType());
    }
}
