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

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/problems")
@RequiredArgsConstructor
public class ProblemController {
    
    private final ProblemServiceClient problemServiceClient;
    
    @GetMapping
    public List<ProblemDto> getProblems(@AuthenticationPrincipal UserContext userContext) {
        log.info("Get problems for organization: {}", userContext.getOrganizationId());
        List<ProblemDto> problems = problemServiceClient.getProblems(userContext.getUserId(), userContext.getOrganizationId());
        log.info("Retrieved problems");
        return problems;
    }
    
    @PostMapping
    public ResponseEntity<ProblemDto> createProblem(
            @Valid @RequestBody CreateProblemRequest request,
            @AuthenticationPrincipal UserContext userContext) {
        log.info("Create problem: {}", request.getTitle());
        ProblemDto problem = problemServiceClient.createProblem(request, userContext.getUserId(), userContext.getOrganizationId());
        log.info("Problem created");
        return ResponseEntity.ok(problem);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<ProblemDto> getProblem(
            @PathVariable String id,
            @AuthenticationPrincipal UserContext userContext) {
        log.info("Get problem: {}", id);
        ProblemDto problem = problemServiceClient.getProblem(id, userContext.getUserId(), userContext.getOrganizationId());
        log.info("Retrieved problem");
        return ResponseEntity.ok(problem);
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<ProblemDto> updateProblem(
            @PathVariable String id,
            @Valid @RequestBody CreateProblemRequest request,
            @AuthenticationPrincipal UserContext userContext) {
        log.info("Update problem: {}", id);
        ProblemDto problem = problemServiceClient.updateProblem(id, request, userContext.getUserId(), userContext.getOrganizationId());
        log.info("Problem updated");
        return ResponseEntity.ok(problem);
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProblem(
            @PathVariable String id,
            @AuthenticationPrincipal UserContext userContext) {
        log.info("Delete problem: {}", id);
        problemServiceClient.deleteProblem(id, userContext.getUserId(), userContext.getOrganizationId());
        log.info("Problem deleted");
        return ResponseEntity.ok().build();
    }
}
