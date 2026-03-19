package com.turaf.bff.controllers;

import com.turaf.bff.clients.ExperimentServiceClient;
import com.turaf.bff.dto.CreateExperimentRequest;
import com.turaf.bff.dto.ExperimentDto;
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

@WebFluxTest(ExperimentController.class)
class ExperimentControllerTest {
    
    @Autowired
    private WebTestClient webTestClient;
    
    @MockBean
    private ExperimentServiceClient experimentServiceClient;
    
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
    void testGetExperiments_Success() {
        ExperimentDto experiment = ExperimentDto.builder()
            .id("exp-123")
            .name("Test Experiment")
            .status("RUNNING")
            .build();
        
        when(experimentServiceClient.getExperiments(anyString(), anyString()))
            .thenReturn(Flux.just(experiment));
        
        webTestClient
            .mutateWith(mockAuthentication(new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                createUserContext(), null, createUserContext().getAuthorities())))
            .get()
            .uri("/api/v1/experiments?organizationId=org-123")
            .exchange()
            .expectStatus().isOk()
            .expectBodyList(ExperimentDto.class)
            .hasSize(1);
    }
    
    @Test
    void testCreateExperiment_Success() {
        CreateExperimentRequest request = CreateExperimentRequest.builder()
            .name("New Experiment")
            .description("Test Description")
            .organizationId("org-123")
            .problemId("prob-1")
            .hypothesisId("hyp-1")
            .build();
        
        ExperimentDto experiment = ExperimentDto.builder()
            .id("exp-456")
            .name("New Experiment")
            .status("DRAFT")
            .build();
        
        when(experimentServiceClient.createExperiment(any(), anyString(), anyString()))
            .thenReturn(Mono.just(experiment));
        
        webTestClient
            .mutateWith(mockAuthentication(new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                createUserContext(), null, createUserContext().getAuthorities())))
            .post()
            .uri("/api/v1/experiments")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.id").isEqualTo("exp-456")
            .jsonPath("$.name").isEqualTo("New Experiment");
    }
    
    @Test
    void testStartExperiment_Success() {
        ExperimentDto experiment = ExperimentDto.builder()
            .id("exp-789")
            .name("Started Experiment")
            .status("RUNNING")
            .build();
        
        when(experimentServiceClient.startExperiment(anyString(), anyString(), anyString()))
            .thenReturn(Mono.just(experiment));
        
        webTestClient
            .mutateWith(mockAuthentication(new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                createUserContext(), null, createUserContext().getAuthorities())))
            .post()
            .uri("/api/v1/experiments/exp-789/start?organizationId=org-123")
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.id").isEqualTo("exp-789")
            .jsonPath("$.status").isEqualTo("RUNNING");
    }
    
    @Test
    void testCompleteExperiment_Success() {
        ExperimentDto experiment = ExperimentDto.builder()
            .id("exp-999")
            .name("Completed Experiment")
            .status("COMPLETED")
            .build();
        
        when(experimentServiceClient.completeExperiment(anyString(), anyString(), anyString()))
            .thenReturn(Mono.just(experiment));
        
        webTestClient
            .mutateWith(mockAuthentication(new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                createUserContext(), null, createUserContext().getAuthorities())))
            .post()
            .uri("/api/v1/experiments/exp-999/complete?organizationId=org-123")
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.id").isEqualTo("exp-999")
            .jsonPath("$.status").isEqualTo("COMPLETED");
    }
}
