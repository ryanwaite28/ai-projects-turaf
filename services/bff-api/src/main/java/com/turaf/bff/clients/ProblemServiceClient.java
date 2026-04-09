package com.turaf.bff.clients;

import com.turaf.bff.dto.CreateProblemRequest;
import com.turaf.bff.dto.ProblemDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

@Slf4j
@Component
public class ProblemServiceClient {
    
    private final RestClient restClient;
    private static final String SERVICE_PATH = "/api/v1/problems";
    
    public ProblemServiceClient(@Qualifier("experimentRestClient") RestClient restClient) {
        this.restClient = restClient;
    }
    
    public List<ProblemDto> getProblems(String userId, String organizationId) {
        log.debug("Calling Experiment Service: GET /problems");
        List<ProblemDto> problems = restClient.get()
            .uri(SERVICE_PATH)
            .header("X-User-Id", userId)
            .header("X-Organization-Id", organizationId)
            .retrieve()
            .body(new ParameterizedTypeReference<List<ProblemDto>>() {});
        log.debug("Retrieved problems");
        return problems;
    }
    
    public ProblemDto createProblem(CreateProblemRequest request, String userId, String organizationId) {
        log.debug("Calling Experiment Service: POST /problems");
        ProblemDto problem = restClient.post()
            .uri(SERVICE_PATH)
            .header("X-User-Id", userId)
            .header("X-Organization-Id", organizationId)
            .body(request)
            .retrieve()
            .body(ProblemDto.class);
        log.debug("Problem created: {}", problem.getId());
        return problem;
    }
    
    public ProblemDto getProblem(String id, String userId, String organizationId) {
        log.debug("Calling Experiment Service: GET /problems/{}", id);
        ProblemDto problem = restClient.get()
            .uri(SERVICE_PATH + "/{id}", id)
            .header("X-User-Id", userId)
            .header("X-Organization-Id", organizationId)
            .retrieve()
            .body(ProblemDto.class);
        log.debug("Retrieved problem: {}", id);
        return problem;
    }
    
    public ProblemDto updateProblem(String id, CreateProblemRequest request, String userId, String organizationId) {
        log.debug("Calling Experiment Service: PUT /problems/{}", id);
        ProblemDto problem = restClient.put()
            .uri(SERVICE_PATH + "/{id}", id)
            .header("X-User-Id", userId)
            .header("X-Organization-Id", organizationId)
            .body(request)
            .retrieve()
            .body(ProblemDto.class);
        log.debug("Problem updated: {}", id);
        return problem;
    }
    
    public void deleteProblem(String id, String userId, String organizationId) {
        log.debug("Calling Experiment Service: DELETE /problems/{}", id);
        restClient.delete()
            .uri(SERVICE_PATH + "/{id}", id)
            .header("X-User-Id", userId)
            .header("X-Organization-Id", organizationId)
            .retrieve()
            .toBodilessEntity();
        log.debug("Problem deleted: {}", id);
    }
}
