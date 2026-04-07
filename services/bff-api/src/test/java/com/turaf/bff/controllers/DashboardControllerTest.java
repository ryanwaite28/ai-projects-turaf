package com.turaf.bff.controllers;

import com.turaf.bff.clients.ExperimentServiceClient;
import com.turaf.bff.clients.IdentityServiceClient;
import com.turaf.bff.clients.MetricsServiceClient;
import com.turaf.bff.clients.OrganizationServiceClient;
import com.turaf.bff.dto.*;
import com.turaf.bff.security.JwtAuthenticationFilter;
import com.turaf.bff.security.UserContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.mockAuthentication;

@WebFluxTest(DashboardController.class)
class DashboardControllerTest {
    
    @Autowired
    private WebTestClient webTestClient;
    
    @MockBean
    private IdentityServiceClient identityServiceClient;
    
    @MockBean
    private OrganizationServiceClient organizationServiceClient;
    
    @MockBean
    private ExperimentServiceClient experimentServiceClient;
    
    @MockBean
    private MetricsServiceClient metricsServiceClient;
    
    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;
    
    private UserContext createUserContext() {
        return UserContext.builder()
            .userId("user-123")
            .organizationId("org-123")
            .email("test@example.com")
            .username("testuser")
            .firstName("Test")
            .lastName("User")
            .build();
    }
    
    @Test
    void testGetDashboardOverview_Success() {
        UserDto user = UserDto.builder()
            .id("user-123")
            .email("test@example.com")
            .username("testuser")
            .firstName("Test")
            .lastName("User")
            .build();
        
        OrganizationDto org = OrganizationDto.builder()
            .id("org-123")
            .name("Test Org")
            .build();
        
        ExperimentDto experiment = ExperimentDto.builder()
            .id("exp-123")
            .name("Test Experiment")
            .status("RUNNING")
            .build();
        
        when(identityServiceClient.getCurrentUser(anyString()))
            .thenReturn(Mono.just(user));
        when(organizationServiceClient.getOrganizations(anyString()))
            .thenReturn(Flux.just(org));
        when(experimentServiceClient.getExperiments(anyString(), anyString()))
            .thenReturn(Flux.just(experiment));
        
        webTestClient
            .mutateWith(mockAuthentication(new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                createUserContext(), null, createUserContext().getAuthorities())))
            .get()
            .uri("/api/v1/dashboard/overview")
            .header("Authorization", "Bearer test-token")
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.user.id").isEqualTo("user-123")
            .jsonPath("$.organizations[0].id").isEqualTo("org-123")
            .jsonPath("$.activeExperiments[0].id").isEqualTo("exp-123")
            .jsonPath("$.totalOrganizations").isEqualTo(1)
            .jsonPath("$.totalActiveExperiments").isEqualTo(1);
    }
    
    @Test
    void testGetDashboardOverview_WithErrors() {
        UserDto user = UserDto.builder()
            .id("user-123")
            .build();
        
        when(identityServiceClient.getCurrentUser(anyString()))
            .thenReturn(Mono.just(user));
        when(organizationServiceClient.getOrganizations(anyString()))
            .thenReturn(Flux.error(new RuntimeException("Service error")));
        when(experimentServiceClient.getExperiments(anyString(), anyString()))
            .thenReturn(Flux.error(new RuntimeException("Service error")));
        
        webTestClient
            .mutateWith(mockAuthentication(new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                createUserContext(), null, createUserContext().getAuthorities())))
            .get()
            .uri("/api/v1/dashboard/overview")
            .header("Authorization", "Bearer test-token")
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.user.id").isEqualTo("user-123")
            .jsonPath("$.organizations").isEmpty()
            .jsonPath("$.activeExperiments").isEmpty()
            .jsonPath("$.totalOrganizations").isEqualTo(0)
            .jsonPath("$.totalActiveExperiments").isEqualTo(0);
    }
    
    @Test
    void testGetExperimentFull_Success() {
        ExperimentDto experiment = ExperimentDto.builder()
            .id("exp-456")
            .name("Full Experiment")
            .status("RUNNING")
            .build();
        
        MetricDto metric1 = MetricDto.builder()
            .id("metric-1")
            .name("clicks")
            .value(100.0)
            .build();
        
        MetricDto metric2 = MetricDto.builder()
            .id("metric-2")
            .name("views")
            .value(1000.0)
            .build();
        
        when(experimentServiceClient.getExperiment(anyString(), anyString(), anyString()))
            .thenReturn(Mono.just(experiment));
        when(metricsServiceClient.getExperimentMetrics(anyString(), anyString(), anyString()))
            .thenReturn(Flux.just(metric1, metric2));
        
        webTestClient
            .mutateWith(mockAuthentication(new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                createUserContext(), null, createUserContext().getAuthorities())))
            .get()
            .uri("/api/v1/dashboard/experiments/exp-456/full?organizationId=org-123")
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.experiment.id").isEqualTo("exp-456")
            .jsonPath("$.metrics[0].id").isEqualTo("metric-1")
            .jsonPath("$.metrics[1].id").isEqualTo("metric-2")
            .jsonPath("$.totalMetrics").isEqualTo(2);
    }
    
    @Test
    void testGetOrganizationSummary_Success() {
        OrganizationDto org = OrganizationDto.builder()
            .id("org-789")
            .name("Summary Org")
            .build();
        
        MemberDto member = MemberDto.builder()
            .id("member-1")
            .userName("John Doe")
            .build();
        
        ExperimentDto experiment = ExperimentDto.builder()
            .id("exp-789")
            .name("Org Experiment")
            .build();
        
        when(organizationServiceClient.getOrganization(anyString(), anyString()))
            .thenReturn(Mono.just(org));
        when(organizationServiceClient.getMembers(anyString(), anyString()))
            .thenReturn(Flux.just(member));
        when(experimentServiceClient.getExperiments(anyString(), anyString()))
            .thenReturn(Flux.just(experiment));
        
        webTestClient
            .mutateWith(mockAuthentication(new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                createUserContext(), null, createUserContext().getAuthorities())))
            .get()
            .uri("/api/v1/dashboard/organizations/org-789/summary")
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.organization.id").isEqualTo("org-789")
            .jsonPath("$.members[0].id").isEqualTo("member-1")
            .jsonPath("$.experiments[0].id").isEqualTo("exp-789")
            .jsonPath("$.totalMembers").isEqualTo(1)
            .jsonPath("$.totalExperiments").isEqualTo(1);
    }
    
    @Test
    void testGetExperimentFull_WithMetricsError() {
        ExperimentDto experiment = ExperimentDto.builder()
            .id("exp-999")
            .name("Experiment with Error")
            .build();
        
        when(experimentServiceClient.getExperiment(anyString(), anyString(), anyString()))
            .thenReturn(Mono.just(experiment));
        when(metricsServiceClient.getExperimentMetrics(anyString(), anyString(), anyString()))
            .thenReturn(Flux.error(new RuntimeException("Metrics service error")));
        
        webTestClient
            .mutateWith(mockAuthentication(new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                createUserContext(), null, createUserContext().getAuthorities())))
            .get()
            .uri("/api/v1/dashboard/experiments/exp-999/full?organizationId=org-123")
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.experiment.id").isEqualTo("exp-999")
            .jsonPath("$.metrics").isEmpty()
            .jsonPath("$.totalMetrics").isEqualTo(0);
    }
}
