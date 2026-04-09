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

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/experiments")
@RequiredArgsConstructor
public class ExperimentController {
    
    private final ExperimentServiceClient experimentServiceClient;
    
    @GetMapping
    public List<ExperimentDto> getExperiments(
            @RequestParam String organizationId,
            @AuthenticationPrincipal UserContext userContext) {
        log.info("Get experiments for organization: {}", organizationId);
        List<ExperimentDto> experiments = experimentServiceClient.getExperiments(organizationId, userContext.getUserId());
        log.info("Retrieved experiments");
        return experiments;
    }
    
    @PostMapping
    public ResponseEntity<ExperimentDto> createExperiment(
            @Valid @RequestBody CreateExperimentRequest request,
            @AuthenticationPrincipal UserContext userContext) {
        log.info("Create experiment: {}", request.getName());
        ExperimentDto exp = experimentServiceClient.createExperiment(
                request, 
                userContext.getUserId(), 
                request.getOrganizationId());
        log.info("Experiment created");
        return ResponseEntity.ok(exp);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<ExperimentDto> getExperiment(
            @PathVariable String id,
            @RequestParam String organizationId,
            @AuthenticationPrincipal UserContext userContext) {
        log.info("Get experiment: {}", id);
        ExperimentDto exp = experimentServiceClient.getExperiment(id, userContext.getUserId(), organizationId);
        log.info("Retrieved experiment");
        return ResponseEntity.ok(exp);
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<ExperimentDto> updateExperiment(
            @PathVariable String id,
            @Valid @RequestBody CreateExperimentRequest request,
            @AuthenticationPrincipal UserContext userContext) {
        log.info("Update experiment: {}", id);
        ExperimentDto exp = experimentServiceClient.updateExperiment(
                id, 
                request, 
                userContext.getUserId(), 
                request.getOrganizationId());
        log.info("Experiment updated");
        return ResponseEntity.ok(exp);
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteExperiment(
            @PathVariable String id,
            @RequestParam String organizationId,
            @AuthenticationPrincipal UserContext userContext) {
        log.info("Delete experiment: {}", id);
        experimentServiceClient.deleteExperiment(id, userContext.getUserId(), organizationId);
        log.info("Experiment deleted");
        return ResponseEntity.ok().build();
    }
    
    @PostMapping("/{id}/start")
    public ResponseEntity<ExperimentDto> startExperiment(
            @PathVariable String id,
            @RequestParam String organizationId,
            @AuthenticationPrincipal UserContext userContext) {
        log.info("Start experiment: {}", id);
        ExperimentDto exp = experimentServiceClient.startExperiment(id, userContext.getUserId(), organizationId);
        log.info("Experiment started");
        return ResponseEntity.ok(exp);
    }
    
    @PostMapping("/{id}/complete")
    public ResponseEntity<ExperimentDto> completeExperiment(
            @PathVariable String id,
            @RequestParam String organizationId,
            @AuthenticationPrincipal UserContext userContext) {
        log.info("Complete experiment: {}", id);
        ExperimentDto exp = experimentServiceClient.completeExperiment(id, userContext.getUserId(), organizationId);
        log.info("Experiment completed");
        return ResponseEntity.ok(exp);
    }
    
    @PostMapping("/{id}/cancel")
    public ResponseEntity<ExperimentDto> cancelExperiment(
            @PathVariable String id,
            @RequestParam String organizationId,
            @AuthenticationPrincipal UserContext userContext) {
        log.info("Cancel experiment: {}", id);
        ExperimentDto exp = experimentServiceClient.cancelExperiment(id, userContext.getUserId(), organizationId);
        log.info("Experiment cancelled");
        return ResponseEntity.ok(exp);
    }
}
