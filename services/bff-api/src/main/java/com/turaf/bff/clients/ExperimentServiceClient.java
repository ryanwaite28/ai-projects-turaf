package com.turaf.bff.clients;

import com.turaf.bff.dto.CreateExperimentRequest;
import com.turaf.bff.dto.ExperimentDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

@Slf4j
@Component
public class ExperimentServiceClient {
    
    private final RestClient restClient;
    private static final String SERVICE_PATH = "/api/v1/experiments";
    
    public ExperimentServiceClient(@Qualifier("experimentRestClient") RestClient restClient) {
        this.restClient = restClient;
    }
    
    public List<ExperimentDto> getExperiments(String organizationId, String userId) {
        log.debug("Calling Experiment Service: GET /experiments for organization: {}", organizationId);
        List<ExperimentDto> experiments = restClient.get()
            .uri(uriBuilder -> uriBuilder
                .path(SERVICE_PATH)
                .queryParam("organizationId", organizationId)
                .build())
            .header("X-User-Id", userId)
            .header("X-Organization-Id", organizationId)
            .retrieve()
            .body(new ParameterizedTypeReference<List<ExperimentDto>>() {});
        log.debug("Retrieved experiments for organization: {}", organizationId);
        return experiments;
    }
    
    public ExperimentDto createExperiment(CreateExperimentRequest request, String userId, String organizationId) {
        log.debug("Calling Experiment Service: POST /experiments");
        ExperimentDto exp = restClient.post()
            .uri(SERVICE_PATH)
            .header("X-User-Id", userId)
            .header("X-Organization-Id", organizationId)
            .body(request)
            .retrieve()
            .body(ExperimentDto.class);
        log.debug("Experiment created: {}", exp.getId());
        return exp;
    }
    
    public ExperimentDto getExperiment(String id, String userId, String organizationId) {
        log.debug("Calling Experiment Service: GET /experiments/{}", id);
        ExperimentDto exp = restClient.get()
            .uri(SERVICE_PATH + "/{id}", id)
            .header("X-User-Id", userId)
            .header("X-Organization-Id", organizationId)
            .retrieve()
            .body(ExperimentDto.class);
        log.debug("Retrieved experiment: {}", id);
        return exp;
    }
    
    public ExperimentDto updateExperiment(String id, CreateExperimentRequest request, String userId, String organizationId) {
        log.debug("Calling Experiment Service: PUT /experiments/{}", id);
        ExperimentDto exp = restClient.put()
            .uri(SERVICE_PATH + "/{id}", id)
            .header("X-User-Id", userId)
            .header("X-Organization-Id", organizationId)
            .body(request)
            .retrieve()
            .body(ExperimentDto.class);
        log.debug("Experiment updated: {}", id);
        return exp;
    }
    
    public void deleteExperiment(String id, String userId, String organizationId) {
        log.debug("Calling Experiment Service: DELETE /experiments/{}", id);
        restClient.delete()
            .uri(SERVICE_PATH + "/{id}", id)
            .header("X-User-Id", userId)
            .header("X-Organization-Id", organizationId)
            .retrieve()
            .toBodilessEntity();
        log.debug("Experiment deleted: {}", id);
    }
    
    public ExperimentDto startExperiment(String id, String userId, String organizationId) {
        log.debug("Calling Experiment Service: POST /experiments/{}/start", id);
        ExperimentDto exp = restClient.post()
            .uri(SERVICE_PATH + "/{id}/start", id)
            .header("X-User-Id", userId)
            .header("X-Organization-Id", organizationId)
            .retrieve()
            .body(ExperimentDto.class);
        log.debug("Experiment started: {}", id);
        return exp;
    }
    
    public ExperimentDto completeExperiment(String id, String userId, String organizationId) {
        log.debug("Calling Experiment Service: POST /experiments/{}/complete", id);
        ExperimentDto exp = restClient.post()
            .uri(SERVICE_PATH + "/{id}/complete", id)
            .header("X-User-Id", userId)
            .header("X-Organization-Id", organizationId)
            .retrieve()
            .body(ExperimentDto.class);
        log.debug("Experiment completed: {}", id);
        return exp;
    }
    
    public ExperimentDto cancelExperiment(String id, String userId, String organizationId) {
        log.debug("Calling Experiment Service: POST /experiments/{}/cancel", id);
        ExperimentDto exp = restClient.post()
            .uri(SERVICE_PATH + "/{id}/cancel", id)
            .header("X-User-Id", userId)
            .header("X-Organization-Id", organizationId)
            .retrieve()
            .body(ExperimentDto.class);
        log.debug("Experiment cancelled: {}", id);
        return exp;
    }
}
