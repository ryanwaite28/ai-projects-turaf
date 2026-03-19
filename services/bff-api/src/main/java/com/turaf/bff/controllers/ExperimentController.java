package com.turaf.bff.controllers;

import com.turaf.bff.clients.ExperimentServiceClient;
import com.turaf.bff.dto.CreateExperimentRequest;
import com.turaf.bff.dto.ExperimentDto;
import com.turaf.bff.security.UserContext;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@RestController
@RequestMapping("/api/v1/experiments")
@RequiredArgsConstructor
public class ExperimentController {
    
    private final ExperimentServiceClient experimentServiceClient;
    
    @GetMapping
    public Flux<ExperimentDto> getExperiments(
            @RequestParam String organizationId,
            @AuthenticationPrincipal UserContext userContext) {
        log.info("Get experiments for organization: {}", organizationId);
        return experimentServiceClient.getExperiments(organizationId, userContext.getUserId())
            .doOnComplete(() -> log.info("Retrieved experiments"))
            .doOnError(error -> log.error("Failed to get experiments", error));
    }
    
    @PostMapping
    public Mono<ResponseEntity<ExperimentDto>> createExperiment(
            @Valid @RequestBody CreateExperimentRequest request,
            @AuthenticationPrincipal UserContext userContext) {
        log.info("Create experiment: {}", request.getName());
        return experimentServiceClient.createExperiment(
                request, 
                userContext.getUserId(), 
                request.getOrganizationId())
            .map(ResponseEntity::ok)
            .doOnSuccess(response -> log.info("Experiment created"))
            .doOnError(error -> log.error("Failed to create experiment", error));
    }
    
    @GetMapping("/{id}")
    public Mono<ResponseEntity<ExperimentDto>> getExperiment(
            @PathVariable String id,
            @RequestParam String organizationId,
            @AuthenticationPrincipal UserContext userContext) {
        log.info("Get experiment: {}", id);
        return experimentServiceClient.getExperiment(id, userContext.getUserId(), organizationId)
            .map(ResponseEntity::ok)
            .doOnError(error -> log.error("Failed to get experiment {}", id, error));
    }
    
    @PutMapping("/{id}")
    public Mono<ResponseEntity<ExperimentDto>> updateExperiment(
            @PathVariable String id,
            @Valid @RequestBody CreateExperimentRequest request,
            @AuthenticationPrincipal UserContext userContext) {
        log.info("Update experiment: {}", id);
        return experimentServiceClient.updateExperiment(
                id, 
                request, 
                userContext.getUserId(), 
                request.getOrganizationId())
            .map(ResponseEntity::ok)
            .doOnSuccess(response -> log.info("Experiment updated"))
            .doOnError(error -> log.error("Failed to update experiment {}", id, error));
    }
    
    @DeleteMapping("/{id}")
    public Mono<ResponseEntity<Void>> deleteExperiment(
            @PathVariable String id,
            @RequestParam String organizationId,
            @AuthenticationPrincipal UserContext userContext) {
        log.info("Delete experiment: {}", id);
        return experimentServiceClient.deleteExperiment(id, userContext.getUserId(), organizationId)
            .map(ResponseEntity::ok)
            .doOnSuccess(response -> log.info("Experiment deleted"))
            .doOnError(error -> log.error("Failed to delete experiment {}", id, error));
    }
    
    @PostMapping("/{id}/start")
    public Mono<ResponseEntity<ExperimentDto>> startExperiment(
            @PathVariable String id,
            @RequestParam String organizationId,
            @AuthenticationPrincipal UserContext userContext) {
        log.info("Start experiment: {}", id);
        return experimentServiceClient.startExperiment(id, userContext.getUserId(), organizationId)
            .map(ResponseEntity::ok)
            .doOnSuccess(response -> log.info("Experiment started"))
            .doOnError(error -> log.error("Failed to start experiment {}", id, error));
    }
    
    @PostMapping("/{id}/complete")
    public Mono<ResponseEntity<ExperimentDto>> completeExperiment(
            @PathVariable String id,
            @RequestParam String organizationId,
            @AuthenticationPrincipal UserContext userContext) {
        log.info("Complete experiment: {}", id);
        return experimentServiceClient.completeExperiment(id, userContext.getUserId(), organizationId)
            .map(ResponseEntity::ok)
            .doOnSuccess(response -> log.info("Experiment completed"))
            .doOnError(error -> log.error("Failed to complete experiment {}", id, error));
    }
}
