package com.turaf.bff.clients;

import com.turaf.bff.dto.CreateHypothesisRequest;
import com.turaf.bff.dto.HypothesisDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

@Slf4j
@Component
public class HypothesisServiceClient {
    
    private final RestClient restClient;
    private static final String SERVICE_PATH = "/api/v1/hypotheses";
    
    public HypothesisServiceClient(@Qualifier("experimentRestClient") RestClient restClient) {
        this.restClient = restClient;
    }
    
    public List<HypothesisDto> getHypotheses(String userId, String organizationId, String problemId) {
        log.debug("Calling Experiment Service: GET /hypotheses");
        
        if (problemId != null && !problemId.isEmpty()) {
            List<HypothesisDto> hypotheses = restClient.get()
                .uri(uriBuilder -> uriBuilder
                    .path(SERVICE_PATH)
                    .queryParam("problemId", problemId)
                    .build())
                .header("X-User-Id", userId)
                .header("X-Organization-Id", organizationId)
                .retrieve()
                .body(new ParameterizedTypeReference<List<HypothesisDto>>() {});
            log.debug("Retrieved hypotheses for problem: {}", problemId);
            return hypotheses;
        } else {
            List<HypothesisDto> hypotheses = restClient.get()
                .uri(SERVICE_PATH)
                .header("X-User-Id", userId)
                .header("X-Organization-Id", organizationId)
                .retrieve()
                .body(new ParameterizedTypeReference<List<HypothesisDto>>() {});
            log.debug("Retrieved all hypotheses");
            return hypotheses;
        }
    }
    
    public HypothesisDto createHypothesis(CreateHypothesisRequest request, String userId, String organizationId) {
        log.debug("Calling Experiment Service: POST /hypotheses");
        HypothesisDto hypothesis = restClient.post()
            .uri(SERVICE_PATH)
            .header("X-User-Id", userId)
            .header("X-Organization-Id", organizationId)
            .body(request)
            .retrieve()
            .body(HypothesisDto.class);
        log.debug("Hypothesis created: {}", hypothesis.getId());
        return hypothesis;
    }
    
    public HypothesisDto getHypothesis(String id, String userId, String organizationId) {
        log.debug("Calling Experiment Service: GET /hypotheses/{}", id);
        HypothesisDto hypothesis = restClient.get()
            .uri(SERVICE_PATH + "/{id}", id)
            .header("X-User-Id", userId)
            .header("X-Organization-Id", organizationId)
            .retrieve()
            .body(HypothesisDto.class);
        log.debug("Retrieved hypothesis: {}", id);
        return hypothesis;
    }
    
    public HypothesisDto updateHypothesis(String id, CreateHypothesisRequest request, String userId, String organizationId) {
        log.debug("Calling Experiment Service: PUT /hypotheses/{}", id);
        HypothesisDto hypothesis = restClient.put()
            .uri(SERVICE_PATH + "/{id}", id)
            .header("X-User-Id", userId)
            .header("X-Organization-Id", organizationId)
            .body(request)
            .retrieve()
            .body(HypothesisDto.class);
        log.debug("Hypothesis updated: {}", id);
        return hypothesis;
    }
    
    public void deleteHypothesis(String id, String userId, String organizationId) {
        log.debug("Calling Experiment Service: DELETE /hypotheses/{}", id);
        restClient.delete()
            .uri(SERVICE_PATH + "/{id}", id)
            .header("X-User-Id", userId)
            .header("X-Organization-Id", organizationId)
            .retrieve()
            .toBodilessEntity();
        log.debug("Hypothesis deleted: {}", id);
    }
}
