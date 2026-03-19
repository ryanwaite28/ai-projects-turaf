package com.turaf.bff.clients;

import com.turaf.bff.dto.CreateExperimentRequest;
import com.turaf.bff.dto.ExperimentDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@RequiredArgsConstructor
public class ExperimentServiceClient {
    
    private final WebClient webClient;
    private static final String SERVICE_PATH = "/experiment";
    
    public Flux<ExperimentDto> getExperiments(String organizationId, String userId) {
        log.debug("Calling Experiment Service: GET /experiments for organization: {}", organizationId);
        return webClient.get()
            .uri(uriBuilder -> uriBuilder
                .path(SERVICE_PATH + "/experiments")
                .queryParam("organizationId", organizationId)
                .build())
            .header("X-User-Id", userId)
            .header("X-Organization-Id", organizationId)
            .retrieve()
            .bodyToFlux(ExperimentDto.class)
            .doOnComplete(() -> log.debug("Retrieved experiments for organization: {}", organizationId))
            .doOnError(error -> log.error("Failed to get experiments for organization: {}", organizationId, error));
    }
    
    public Mono<ExperimentDto> createExperiment(CreateExperimentRequest request, String userId, String organizationId) {
        log.debug("Calling Experiment Service: POST /experiments");
        return webClient.post()
            .uri(SERVICE_PATH + "/experiments")
            .header("X-User-Id", userId)
            .header("X-Organization-Id", organizationId)
            .bodyValue(request)
            .retrieve()
            .bodyToMono(ExperimentDto.class)
            .doOnSuccess(exp -> log.debug("Experiment created: {}", exp.getId()))
            .doOnError(error -> log.error("Failed to create experiment", error));
    }
    
    public Mono<ExperimentDto> getExperiment(String id, String userId, String organizationId) {
        log.debug("Calling Experiment Service: GET /experiments/{}", id);
        return webClient.get()
            .uri(SERVICE_PATH + "/experiments/{id}", id)
            .header("X-User-Id", userId)
            .header("X-Organization-Id", organizationId)
            .retrieve()
            .bodyToMono(ExperimentDto.class)
            .doOnSuccess(exp -> log.debug("Retrieved experiment: {}", id))
            .doOnError(error -> log.error("Failed to get experiment: {}", id, error));
    }
    
    public Mono<ExperimentDto> updateExperiment(String id, CreateExperimentRequest request, String userId, String organizationId) {
        log.debug("Calling Experiment Service: PUT /experiments/{}", id);
        return webClient.put()
            .uri(SERVICE_PATH + "/experiments/{id}", id)
            .header("X-User-Id", userId)
            .header("X-Organization-Id", organizationId)
            .bodyValue(request)
            .retrieve()
            .bodyToMono(ExperimentDto.class)
            .doOnSuccess(exp -> log.debug("Experiment updated: {}", id))
            .doOnError(error -> log.error("Failed to update experiment: {}", id, error));
    }
    
    public Mono<Void> deleteExperiment(String id, String userId, String organizationId) {
        log.debug("Calling Experiment Service: DELETE /experiments/{}", id);
        return webClient.delete()
            .uri(SERVICE_PATH + "/experiments/{id}", id)
            .header("X-User-Id", userId)
            .header("X-Organization-Id", organizationId)
            .retrieve()
            .bodyToMono(Void.class)
            .doOnSuccess(v -> log.debug("Experiment deleted: {}", id))
            .doOnError(error -> log.error("Failed to delete experiment: {}", id, error));
    }
    
    public Mono<ExperimentDto> startExperiment(String id, String userId, String organizationId) {
        log.debug("Calling Experiment Service: POST /experiments/{}/start", id);
        return webClient.post()
            .uri(SERVICE_PATH + "/experiments/{id}/start", id)
            .header("X-User-Id", userId)
            .header("X-Organization-Id", organizationId)
            .retrieve()
            .bodyToMono(ExperimentDto.class)
            .doOnSuccess(exp -> log.debug("Experiment started: {}", id))
            .doOnError(error -> log.error("Failed to start experiment: {}", id, error));
    }
    
    public Mono<ExperimentDto> completeExperiment(String id, String userId, String organizationId) {
        log.debug("Calling Experiment Service: POST /experiments/{}/complete", id);
        return webClient.post()
            .uri(SERVICE_PATH + "/experiments/{id}/complete", id)
            .header("X-User-Id", userId)
            .header("X-Organization-Id", organizationId)
            .retrieve()
            .bodyToMono(ExperimentDto.class)
            .doOnSuccess(exp -> log.debug("Experiment completed: {}", id))
            .doOnError(error -> log.error("Failed to complete experiment: {}", id, error));
    }
}
