package com.turaf.bff.clients;

import com.turaf.bff.dto.MetricDto;
import com.turaf.bff.dto.RecordMetricRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@Component
public class MetricsServiceClient {
    
    private final WebClient webClient;
    private static final String SERVICE_PATH = "/api/v1";
    
    public MetricsServiceClient(@Qualifier("metricsWebClient") WebClient webClient) {
        this.webClient = webClient;
    }
    
    public Mono<MetricDto> recordMetric(RecordMetricRequest request, String userId, String organizationId) {
        log.debug("Calling Metrics Service: POST /metrics for experiment: {}", request.getExperimentId());
        return webClient.post()
            .uri(SERVICE_PATH + "/metrics")
            .header("X-User-Id", userId)
            .header("X-Organization-Id", organizationId)
            .bodyValue(request)
            .retrieve()
            .bodyToMono(MetricDto.class)
            .doOnSuccess(metric -> log.debug("Metric recorded: {}", metric.getId()))
            .doOnError(error -> log.error("Failed to record metric", error));
    }
    
    public Flux<MetricDto> getExperimentMetrics(String experimentId, String userId, String organizationId) {
        log.debug("Calling Metrics Service: GET /metrics?experimentId={}", experimentId);
        return webClient.get()
            .uri(uriBuilder -> uriBuilder
                .path(SERVICE_PATH + "/metrics")
                .queryParam("experimentId", experimentId)
                .build())
            .header("X-User-Id", userId)
            .header("X-Organization-Id", organizationId)
            .retrieve()
            .bodyToFlux(MetricDto.class)
            .doOnComplete(() -> log.debug("Retrieved metrics for experiment: {}", experimentId))
            .doOnError(error -> log.error("Failed to get metrics for experiment: {}", experimentId, error));
    }
    
    public Mono<MetricDto> getMetric(String id, String userId, String organizationId) {
        log.debug("Calling Metrics Service: GET /metrics/{}", id);
        return webClient.get()
            .uri(SERVICE_PATH + "/metrics/{id}", id)
            .header("X-User-Id", userId)
            .header("X-Organization-Id", organizationId)
            .retrieve()
            .bodyToMono(MetricDto.class)
            .doOnSuccess(metric -> log.debug("Retrieved metric: {}", id))
            .doOnError(error -> log.error("Failed to get metric: {}", id, error));
    }
    
    public Mono<Void> deleteMetric(String id, String userId, String organizationId) {
        log.debug("Calling Metrics Service: DELETE /metrics/{}", id);
        return webClient.delete()
            .uri(SERVICE_PATH + "/metrics/{id}", id)
            .header("X-User-Id", userId)
            .header("X-Organization-Id", organizationId)
            .retrieve()
            .bodyToMono(Void.class)
            .doOnSuccess(v -> log.debug("Metric deleted: {}", id))
            .doOnError(error -> log.error("Failed to delete metric: {}", id, error));
    }
}
