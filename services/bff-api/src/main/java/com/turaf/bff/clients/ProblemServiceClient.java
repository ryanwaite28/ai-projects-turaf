package com.turaf.bff.clients;

import com.turaf.bff.dto.CreateProblemRequest;
import com.turaf.bff.dto.ProblemDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@Component
public class ProblemServiceClient {
    
    private final WebClient webClient;
    private static final String SERVICE_PATH = "/experiment";
    
    public ProblemServiceClient(@Qualifier("experimentWebClient") WebClient webClient) {
        this.webClient = webClient;
    }
    
    public Flux<ProblemDto> getProblems(String userId, String organizationId) {
        log.debug("Calling Experiment Service: GET /problems");
        return webClient.get()
            .uri(SERVICE_PATH + "/problems")
            .header("X-User-Id", userId)
            .header("X-Organization-Id", organizationId)
            .retrieve()
            .bodyToFlux(ProblemDto.class)
            .doOnComplete(() -> log.debug("Retrieved problems"))
            .doOnError(error -> log.error("Failed to get problems", error));
    }
    
    public Mono<ProblemDto> createProblem(CreateProblemRequest request, String userId, String organizationId) {
        log.debug("Calling Experiment Service: POST /problems");
        return webClient.post()
            .uri(SERVICE_PATH + "/problems")
            .header("X-User-Id", userId)
            .header("X-Organization-Id", organizationId)
            .bodyValue(request)
            .retrieve()
            .bodyToMono(ProblemDto.class)
            .doOnSuccess(problem -> log.debug("Problem created: {}", problem.getId()))
            .doOnError(error -> log.error("Failed to create problem", error));
    }
    
    public Mono<ProblemDto> getProblem(String id, String userId, String organizationId) {
        log.debug("Calling Experiment Service: GET /problems/{}", id);
        return webClient.get()
            .uri(SERVICE_PATH + "/problems/{id}", id)
            .header("X-User-Id", userId)
            .header("X-Organization-Id", organizationId)
            .retrieve()
            .bodyToMono(ProblemDto.class)
            .doOnSuccess(problem -> log.debug("Retrieved problem: {}", id))
            .doOnError(error -> log.error("Failed to get problem: {}", id, error));
    }
    
    public Mono<ProblemDto> updateProblem(String id, CreateProblemRequest request, String userId, String organizationId) {
        log.debug("Calling Experiment Service: PUT /problems/{}", id);
        return webClient.put()
            .uri(SERVICE_PATH + "/problems/{id}", id)
            .header("X-User-Id", userId)
            .header("X-Organization-Id", organizationId)
            .bodyValue(request)
            .retrieve()
            .bodyToMono(ProblemDto.class)
            .doOnSuccess(problem -> log.debug("Problem updated: {}", id))
            .doOnError(error -> log.error("Failed to update problem: {}", id, error));
    }
    
    public Mono<Void> deleteProblem(String id, String userId, String organizationId) {
        log.debug("Calling Experiment Service: DELETE /problems/{}", id);
        return webClient.delete()
            .uri(SERVICE_PATH + "/problems/{id}", id)
            .header("X-User-Id", userId)
            .header("X-Organization-Id", organizationId)
            .retrieve()
            .bodyToMono(Void.class)
            .doOnSuccess(v -> log.debug("Problem deleted: {}", id))
            .doOnError(error -> log.error("Failed to delete problem: {}", id, error));
    }
}
