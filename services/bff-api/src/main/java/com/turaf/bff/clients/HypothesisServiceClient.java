package com.turaf.bff.clients;

import com.turaf.bff.dto.CreateHypothesisRequest;
import com.turaf.bff.dto.HypothesisDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@Component
public class HypothesisServiceClient {
    
    private final WebClient webClient;
    private static final String SERVICE_PATH = "/api/v1";
    
    public HypothesisServiceClient(@Qualifier("experimentWebClient") WebClient webClient) {
        this.webClient = webClient;
    }
    
    public Flux<HypothesisDto> getHypotheses(String userId, String organizationId, String problemId) {
        log.debug("Calling Experiment Service: GET /hypotheses");
        
        WebClient.RequestHeadersUriSpec<?> spec = webClient.get();
        
        if (problemId != null && !problemId.isEmpty()) {
            return spec.uri(uriBuilder -> uriBuilder
                    .path(SERVICE_PATH + "/hypotheses")
                    .queryParam("problemId", problemId)
                    .build())
                .header("X-User-Id", userId)
                .header("X-Organization-Id", organizationId)
                .retrieve()
                .bodyToFlux(HypothesisDto.class)
                .doOnComplete(() -> log.debug("Retrieved hypotheses for problem: {}", problemId))
                .doOnError(error -> log.error("Failed to get hypotheses", error));
        } else {
            return spec.uri(SERVICE_PATH + "/hypotheses")
                .header("X-User-Id", userId)
                .header("X-Organization-Id", organizationId)
                .retrieve()
                .bodyToFlux(HypothesisDto.class)
                .doOnComplete(() -> log.debug("Retrieved all hypotheses"))
                .doOnError(error -> log.error("Failed to get hypotheses", error));
        }
    }
    
    public Mono<HypothesisDto> createHypothesis(CreateHypothesisRequest request, String userId, String organizationId) {
        log.debug("Calling Experiment Service: POST /hypotheses");
        return webClient.post()
            .uri(SERVICE_PATH + "/hypotheses")
            .header("X-User-Id", userId)
            .header("X-Organization-Id", organizationId)
            .bodyValue(request)
            .retrieve()
            .bodyToMono(HypothesisDto.class)
            .doOnSuccess(hypothesis -> log.debug("Hypothesis created: {}", hypothesis.getId()))
            .doOnError(error -> log.error("Failed to create hypothesis", error));
    }
    
    public Mono<HypothesisDto> getHypothesis(String id, String userId, String organizationId) {
        log.debug("Calling Experiment Service: GET /hypotheses/{}", id);
        return webClient.get()
            .uri(SERVICE_PATH + "/hypotheses/{id}", id)
            .header("X-User-Id", userId)
            .header("X-Organization-Id", organizationId)
            .retrieve()
            .bodyToMono(HypothesisDto.class)
            .doOnSuccess(hypothesis -> log.debug("Retrieved hypothesis: {}", id))
            .doOnError(error -> log.error("Failed to get hypothesis: {}", id, error));
    }
    
    public Mono<HypothesisDto> updateHypothesis(String id, CreateHypothesisRequest request, String userId, String organizationId) {
        log.debug("Calling Experiment Service: PUT /hypotheses/{}", id);
        return webClient.put()
            .uri(SERVICE_PATH + "/hypotheses/{id}", id)
            .header("X-User-Id", userId)
            .header("X-Organization-Id", organizationId)
            .bodyValue(request)
            .retrieve()
            .bodyToMono(HypothesisDto.class)
            .doOnSuccess(hypothesis -> log.debug("Hypothesis updated: {}", id))
            .doOnError(error -> log.error("Failed to update hypothesis: {}", id, error));
    }
    
    public Mono<Void> deleteHypothesis(String id, String userId, String organizationId) {
        log.debug("Calling Experiment Service: DELETE /hypotheses/{}", id);
        return webClient.delete()
            .uri(SERVICE_PATH + "/hypotheses/{id}", id)
            .header("X-User-Id", userId)
            .header("X-Organization-Id", organizationId)
            .retrieve()
            .bodyToMono(Void.class)
            .doOnSuccess(v -> log.debug("Hypothesis deleted: {}", id))
            .doOnError(error -> log.error("Failed to delete hypothesis: {}", id, error));
    }
}
