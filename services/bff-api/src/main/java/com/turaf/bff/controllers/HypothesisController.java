package com.turaf.bff.controllers;

import com.turaf.bff.clients.HypothesisServiceClient;
import com.turaf.bff.dto.CreateHypothesisRequest;
import com.turaf.bff.dto.HypothesisDto;
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
@RequestMapping("/api/v1/hypotheses")
@RequiredArgsConstructor
public class HypothesisController {
    
    private final HypothesisServiceClient hypothesisServiceClient;
    
    @GetMapping
    public Flux<HypothesisDto> getHypotheses(
            @RequestParam(required = false) String problemId,
            @AuthenticationPrincipal UserContext userContext) {
        log.info("Get hypotheses for organization: {}", userContext.getOrganizationId());
        return hypothesisServiceClient.getHypotheses(userContext.getUserId(), userContext.getOrganizationId(), problemId)
            .doOnComplete(() -> log.info("Retrieved hypotheses"))
            .doOnError(error -> log.error("Failed to get hypotheses", error));
    }
    
    @PostMapping
    public Mono<ResponseEntity<HypothesisDto>> createHypothesis(
            @Valid @RequestBody CreateHypothesisRequest request,
            @AuthenticationPrincipal UserContext userContext) {
        log.info("Create hypothesis for problem: {}", request.getProblemId());
        return hypothesisServiceClient.createHypothesis(request, userContext.getUserId(), userContext.getOrganizationId())
            .map(ResponseEntity::ok)
            .doOnSuccess(response -> log.info("Hypothesis created"))
            .doOnError(error -> log.error("Failed to create hypothesis", error));
    }
    
    @GetMapping("/{id}")
    public Mono<ResponseEntity<HypothesisDto>> getHypothesis(
            @PathVariable String id,
            @AuthenticationPrincipal UserContext userContext) {
        log.info("Get hypothesis: {}", id);
        return hypothesisServiceClient.getHypothesis(id, userContext.getUserId(), userContext.getOrganizationId())
            .map(ResponseEntity::ok)
            .doOnError(error -> log.error("Failed to get hypothesis {}", id, error));
    }
    
    @PutMapping("/{id}")
    public Mono<ResponseEntity<HypothesisDto>> updateHypothesis(
            @PathVariable String id,
            @Valid @RequestBody CreateHypothesisRequest request,
            @AuthenticationPrincipal UserContext userContext) {
        log.info("Update hypothesis: {}", id);
        return hypothesisServiceClient.updateHypothesis(id, request, userContext.getUserId(), userContext.getOrganizationId())
            .map(ResponseEntity::ok)
            .doOnSuccess(response -> log.info("Hypothesis updated"))
            .doOnError(error -> log.error("Failed to update hypothesis {}", id, error));
    }
    
    @DeleteMapping("/{id}")
    public Mono<ResponseEntity<Void>> deleteHypothesis(
            @PathVariable String id,
            @AuthenticationPrincipal UserContext userContext) {
        log.info("Delete hypothesis: {}", id);
        return hypothesisServiceClient.deleteHypothesis(id, userContext.getUserId(), userContext.getOrganizationId())
            .map(ResponseEntity::ok)
            .doOnSuccess(response -> log.info("Hypothesis deleted"))
            .doOnError(error -> log.error("Failed to delete hypothesis {}", id, error));
    }
}
