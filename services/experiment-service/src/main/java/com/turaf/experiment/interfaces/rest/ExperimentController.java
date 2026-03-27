package com.turaf.experiment.interfaces.rest;

import com.turaf.common.security.AuthorizationService;
import com.turaf.common.security.UserPrincipal;
import com.turaf.experiment.application.ExperimentService;
import com.turaf.experiment.application.dto.CreateExperimentRequest;
import com.turaf.experiment.application.dto.ExperimentDto;
import com.turaf.experiment.application.dto.UpdateExperimentRequest;
import com.turaf.experiment.domain.ExperimentId;
import com.turaf.experiment.domain.ExperimentStatus;
import com.turaf.experiment.domain.HypothesisId;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/experiments")
@PreAuthorize("isAuthenticated()")
public class ExperimentController {
    
    private final ExperimentService experimentService;
    private final AuthorizationService authorizationService;

    public ExperimentController(ExperimentService experimentService,
                                AuthorizationService authorizationService) {
        this.experimentService = experimentService;
        this.authorizationService = authorizationService;
    }

    @PostMapping
    public ResponseEntity<ExperimentDto> createExperiment(
            @Valid @RequestBody CreateExperimentRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        authorizationService.validateTenantAccess(principal);
        ExperimentDto experiment = experimentService.createExperiment(request, principal.getUserId());
        return ResponseEntity.status(HttpStatus.CREATED).body(experiment);
    }

    @GetMapping
    public ResponseEntity<List<ExperimentDto>> getExperiments(
            @RequestParam(required = false) String hypothesisId,
            @RequestParam(required = false) String status,
            @AuthenticationPrincipal UserPrincipal principal) {
        authorizationService.validateTenantAccess(principal);
        
        if (hypothesisId != null) {
            List<ExperimentDto> experiments = experimentService.getExperimentsByHypothesis(
                HypothesisId.of(hypothesisId)
            );
            return ResponseEntity.ok(experiments);
        }
        
        if (status != null) {
            List<ExperimentDto> experiments = experimentService.getExperimentsByStatus(
                ExperimentStatus.valueOf(status)
            );
            return ResponseEntity.ok(experiments);
        }
        
        List<ExperimentDto> experiments = experimentService.getAllExperiments();
        return ResponseEntity.ok(experiments);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ExperimentDto> getExperiment(
            @PathVariable String id,
            @AuthenticationPrincipal UserPrincipal principal) {
        authorizationService.validateTenantAccess(principal);
        ExperimentDto experiment = experimentService.getExperiment(ExperimentId.of(id));
        return ResponseEntity.ok(experiment);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ExperimentDto> updateExperiment(
            @PathVariable String id,
            @Valid @RequestBody UpdateExperimentRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        authorizationService.validateTenantAccess(principal);
        ExperimentDto experiment = experimentService.updateExperiment(ExperimentId.of(id), request);
        return ResponseEntity.ok(experiment);
    }

    @PostMapping("/{id}/start")
    public ResponseEntity<ExperimentDto> startExperiment(
            @PathVariable String id,
            @AuthenticationPrincipal UserPrincipal principal) {
        authorizationService.validateTenantAccess(principal);
        ExperimentDto experiment = experimentService.startExperiment(ExperimentId.of(id));
        return ResponseEntity.ok(experiment);
    }

    @PostMapping("/{id}/complete")
    public ResponseEntity<ExperimentDto> completeExperiment(
            @PathVariable String id,
            @AuthenticationPrincipal UserPrincipal principal) {
        authorizationService.validateTenantAccess(principal);
        ExperimentDto experiment = experimentService.completeExperiment(ExperimentId.of(id));
        return ResponseEntity.ok(experiment);
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<ExperimentDto> cancelExperiment(
            @PathVariable String id,
            @AuthenticationPrincipal UserPrincipal principal) {
        authorizationService.validateTenantAccess(principal);
        ExperimentDto experiment = experimentService.cancelExperiment(ExperimentId.of(id));
        return ResponseEntity.ok(experiment);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteExperiment(
            @PathVariable String id,
            @AuthenticationPrincipal UserPrincipal principal) {
        authorizationService.validateTenantAccess(principal);
        experimentService.deleteExperiment(ExperimentId.of(id));
        return ResponseEntity.noContent().build();
    }
}
