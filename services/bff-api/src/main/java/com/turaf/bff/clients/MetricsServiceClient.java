package com.turaf.bff.clients;

import com.turaf.bff.dto.MetricDto;
import com.turaf.bff.dto.RecordMetricRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

@Slf4j
@Component
public class MetricsServiceClient {
    
    private final RestClient restClient;
    private static final String SERVICE_PATH = "/api/v1/metrics";
    
    public MetricsServiceClient(@Qualifier("metricsRestClient") RestClient restClient) {
        this.restClient = restClient;
    }
    
    public MetricDto recordMetric(RecordMetricRequest request, String userId, String organizationId) {
        log.debug("Calling Metrics Service: POST /metrics for experiment: {}", request.getExperimentId());
        MetricDto metric = restClient.post()
            .uri(SERVICE_PATH)
            .header("X-User-Id", userId)
            .header("X-Organization-Id", organizationId)
            .body(request)
            .retrieve()
            .body(MetricDto.class);
        log.debug("Metric recorded: {}", metric.getId());
        return metric;
    }
    
    public List<MetricDto> getExperimentMetrics(String experimentId, String userId, String organizationId) {
        log.debug("Calling Metrics Service: GET /metrics?experimentId={}", experimentId);
        List<MetricDto> metrics = restClient.get()
            .uri(uriBuilder -> uriBuilder
                .path(SERVICE_PATH)
                .queryParam("experimentId", experimentId)
                .build())
            .header("X-User-Id", userId)
            .header("X-Organization-Id", organizationId)
            .retrieve()
            .body(new ParameterizedTypeReference<List<MetricDto>>() {});
        log.debug("Retrieved metrics for experiment: {}", experimentId);
        return metrics;
    }
    
    public MetricDto getMetric(String id, String userId, String organizationId) {
        log.debug("Calling Metrics Service: GET /metrics/{}", id);
        MetricDto metric = restClient.get()
            .uri(SERVICE_PATH + "/{id}", id)
            .header("X-User-Id", userId)
            .header("X-Organization-Id", organizationId)
            .retrieve()
            .body(MetricDto.class);
        log.debug("Retrieved metric: {}", id);
        return metric;
    }
    
    public void deleteMetric(String id, String userId, String organizationId) {
        log.debug("Calling Metrics Service: DELETE /metrics/{}", id);
        restClient.delete()
            .uri(SERVICE_PATH + "/{id}", id)
            .header("X-User-Id", userId)
            .header("X-Organization-Id", organizationId)
            .retrieve()
            .toBodilessEntity();
        log.debug("Metric deleted: {}", id);
    }
}
