package com.turaf.bff.controllers;

import com.turaf.bff.clients.ProblemServiceClient;
import com.turaf.bff.dto.CreateProblemRequest;
import com.turaf.bff.dto.ProblemDto;
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
@RequestMapping("/api/v1/problems")
@RequiredArgsConstructor
public class ProblemController {
    
    private final ProblemServiceClient problemServiceClient;
    
    @GetMapping
    public Flux<ProblemDto> getProblems(@AuthenticationPrincipal UserContext userContext) {
        log.info("Get problems for organization: {}", userContext.getOrganizationId());
        return problemServiceClient.getProblems(userContext.getUserId(), userContext.getOrganizationId())
            .doOnComplete(() -> log.info("Retrieved problems"))
            .doOnError(error -> log.error("Failed to get problems", error));
    }
    
    @PostMapping
    public Mono<ResponseEntity<ProblemDto>> createProblem(
            @Valid @RequestBody CreateProblemRequest request,
            @AuthenticationPrincipal UserContext userContext) {
        log.info("Create problem: {}", request.getTitle());
        return problemServiceClient.createProblem(request, userContext.getUserId(), userContext.getOrganizationId())
            .map(ResponseEntity::ok)
            .doOnSuccess(response -> log.info("Problem created"))
            .doOnError(error -> log.error("Failed to create problem", error));
    }
    
    @GetMapping("/{id}")
    public Mono<ResponseEntity<ProblemDto>> getProblem(
            @PathVariable String id,
            @AuthenticationPrincipal UserContext userContext) {
        log.info("Get problem: {}", id);
        return problemServiceClient.getProblem(id, userContext.getUserId(), userContext.getOrganizationId())
            .map(ResponseEntity::ok)
            .doOnError(error -> log.error("Failed to get problem {}", id, error));
    }
    
    @PutMapping("/{id}")
    public Mono<ResponseEntity<ProblemDto>> updateProblem(
            @PathVariable String id,
            @Valid @RequestBody CreateProblemRequest request,
            @AuthenticationPrincipal UserContext userContext) {
        log.info("Update problem: {}", id);
        return problemServiceClient.updateProblem(id, request, userContext.getUserId(), userContext.getOrganizationId())
            .map(ResponseEntity::ok)
            .doOnSuccess(response -> log.info("Problem updated"))
            .doOnError(error -> log.error("Failed to update problem {}", id, error));
    }
    
    @DeleteMapping("/{id}")
    public Mono<ResponseEntity<Void>> deleteProblem(
            @PathVariable String id,
            @AuthenticationPrincipal UserContext userContext) {
        log.info("Delete problem: {}", id);
        return problemServiceClient.deleteProblem(id, userContext.getUserId(), userContext.getOrganizationId())
            .map(ResponseEntity::ok)
            .doOnSuccess(response -> log.info("Problem deleted"))
            .doOnError(error -> log.error("Failed to delete problem {}", id, error));
    }
}
