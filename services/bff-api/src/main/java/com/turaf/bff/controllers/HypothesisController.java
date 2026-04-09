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

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/hypotheses")
@RequiredArgsConstructor
public class HypothesisController {
    
    private final HypothesisServiceClient hypothesisServiceClient;
    
    @GetMapping
    public List<HypothesisDto> getHypotheses(
            @RequestParam(required = false) String problemId,
            @AuthenticationPrincipal UserContext userContext) {
        log.info("Get hypotheses for organization: {}", userContext.getOrganizationId());
        List<HypothesisDto> hypotheses = hypothesisServiceClient.getHypotheses(userContext.getUserId(), userContext.getOrganizationId(), problemId);
        log.info("Retrieved hypotheses");
        return hypotheses;
    }
    
    @PostMapping
    public ResponseEntity<HypothesisDto> createHypothesis(
            @Valid @RequestBody CreateHypothesisRequest request,
            @AuthenticationPrincipal UserContext userContext) {
        log.info("Create hypothesis for problem: {}", request.getProblemId());
        HypothesisDto hypothesis = hypothesisServiceClient.createHypothesis(request, userContext.getUserId(), userContext.getOrganizationId());
        log.info("Hypothesis created");
        return ResponseEntity.ok(hypothesis);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<HypothesisDto> getHypothesis(
            @PathVariable String id,
            @AuthenticationPrincipal UserContext userContext) {
        log.info("Get hypothesis: {}", id);
        HypothesisDto hypothesis = hypothesisServiceClient.getHypothesis(id, userContext.getUserId(), userContext.getOrganizationId());
        log.info("Retrieved hypothesis");
        return ResponseEntity.ok(hypothesis);
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<HypothesisDto> updateHypothesis(
            @PathVariable String id,
            @Valid @RequestBody CreateHypothesisRequest request,
            @AuthenticationPrincipal UserContext userContext) {
        log.info("Update hypothesis: {}", id);
        HypothesisDto hypothesis = hypothesisServiceClient.updateHypothesis(id, request, userContext.getUserId(), userContext.getOrganizationId());
        log.info("Hypothesis updated");
        return ResponseEntity.ok(hypothesis);
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteHypothesis(
            @PathVariable String id,
            @AuthenticationPrincipal UserContext userContext) {
        log.info("Delete hypothesis: {}", id);
        hypothesisServiceClient.deleteHypothesis(id, userContext.getUserId(), userContext.getOrganizationId());
        log.info("Hypothesis deleted");
        return ResponseEntity.ok().build();
    }
}
