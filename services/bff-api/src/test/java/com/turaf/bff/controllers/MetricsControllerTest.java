package com.turaf.bff.controllers;

import com.turaf.bff.clients.MetricsServiceClient;
import com.turaf.bff.dto.MetricDto;
import com.turaf.bff.dto.RecordMetricRequest;
import com.turaf.bff.security.JwtAuthenticationFilter;
import com.turaf.bff.security.UserContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.mockAuthentication;

@WebFluxTest(MetricsController.class)
class MetricsControllerTest {
    
    @Autowired
    private WebTestClient webTestClient;
    
    @MockBean
    private MetricsServiceClient metricsServiceClient;
    
    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;
    
    private UserContext createUserContext() {
        return UserContext.builder()
            .userId("user-123")
            .organizationId("org-123")
            .email("test@example.com")
            .name("Test User")
            .build();
    }
    
    @Test
    void testRecordMetric_Success() {
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
            .thenReturn(Mono.just(metric));
        
        webTestClient
            .mutateWith(mockAuthentication(new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                createUserContext(), null, createUserContext().getAuthorities())))
            .post()
            .uri("/api/v1/metrics?organizationId=org-123")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.id").isEqualTo("metric-123")
            .jsonPath("$.name").isEqualTo("conversion_rate");
    }
    
    @Test
    void testGetExperimentMetrics_Success() {
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
            .thenReturn(Flux.just(metric1, metric2));
        
        webTestClient
            .mutateWith(mockAuthentication(new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                createUserContext(), null, createUserContext().getAuthorities())))
            .get()
            .uri("/api/v1/metrics/experiments/exp-456?organizationId=org-123")
            .exchange()
            .expectStatus().isOk()
            .expectBodyList(MetricDto.class)
            .hasSize(2);
    }
    
    @Test
    void testGetMetric_Success() {
        MetricDto metric = MetricDto.builder()
            .id("metric-789")
            .experimentId("exp-789")
            .name("revenue")
            .value(5000.0)
            .build();
        
        when(metricsServiceClient.getMetric(anyString(), anyString(), anyString()))
            .thenReturn(Mono.just(metric));
        
        webTestClient
            .mutateWith(mockAuthentication(new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                createUserContext(), null, createUserContext().getAuthorities())))
            .get()
            .uri("/api/v1/metrics/metric-789?organizationId=org-123")
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.id").isEqualTo("metric-789")
            .jsonPath("$.name").isEqualTo("revenue");
    }
    
    @Test
    void testDeleteMetric_Success() {
        when(metricsServiceClient.deleteMetric(anyString(), anyString(), anyString()))
            .thenReturn(Mono.empty());
        
        webTestClient
            .mutateWith(mockAuthentication(new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                createUserContext(), null, createUserContext().getAuthorities())))
            .delete()
            .uri("/api/v1/metrics/metric-999?organizationId=org-123")
            .exchange()
            .expectStatus().isOk();
    }
}
