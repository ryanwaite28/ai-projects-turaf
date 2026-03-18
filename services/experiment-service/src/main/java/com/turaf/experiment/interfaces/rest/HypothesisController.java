package com.turaf.experiment.interfaces.rest;

import com.turaf.common.security.UserPrincipal;
import com.turaf.experiment.application.HypothesisService;
import com.turaf.experiment.application.dto.CreateHypothesisRequest;
import com.turaf.experiment.application.dto.HypothesisDto;
import com.turaf.experiment.application.dto.UpdateHypothesisRequest;
import com.turaf.experiment.domain.HypothesisId;
import com.turaf.experiment.domain.ProblemId;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/hypotheses")
@PreAuthorize("isAuthenticated()")
public class HypothesisController {
    
    private final HypothesisService hypothesisService;

    public HypothesisController(HypothesisService hypothesisService) {
        this.hypothesisService = hypothesisService;
    }

    @PostMapping
    public ResponseEntity<HypothesisDto> createHypothesis(
            @Valid @RequestBody CreateHypothesisRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        HypothesisDto hypothesis = hypothesisService.createHypothesis(request, principal.getUserId());
        return ResponseEntity.status(HttpStatus.CREATED).body(hypothesis);
    }

    @GetMapping
    public ResponseEntity<List<HypothesisDto>> getHypotheses(
            @RequestParam(required = false) String problemId) {
        
        if (problemId != null) {
            List<HypothesisDto> hypotheses = hypothesisService.getHypothesesByProblem(ProblemId.of(problemId));
            return ResponseEntity.ok(hypotheses);
        }
        
        List<HypothesisDto> hypotheses = hypothesisService.getAllHypotheses();
        return ResponseEntity.ok(hypotheses);
    }

    @GetMapping("/{id}")
    public ResponseEntity<HypothesisDto> getHypothesis(@PathVariable String id) {
        HypothesisDto hypothesis = hypothesisService.getHypothesis(HypothesisId.of(id));
        return ResponseEntity.ok(hypothesis);
    }

    @PutMapping("/{id}")
    public ResponseEntity<HypothesisDto> updateHypothesis(
            @PathVariable String id,
            @Valid @RequestBody UpdateHypothesisRequest request) {
        HypothesisDto hypothesis = hypothesisService.updateHypothesis(HypothesisId.of(id), request);
        return ResponseEntity.ok(hypothesis);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteHypothesis(@PathVariable String id) {
        hypothesisService.deleteHypothesis(HypothesisId.of(id));
        return ResponseEntity.noContent().build();
    }
}
