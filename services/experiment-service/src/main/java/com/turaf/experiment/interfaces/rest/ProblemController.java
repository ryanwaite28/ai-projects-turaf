package com.turaf.experiment.interfaces.rest;

import com.turaf.common.security.UserPrincipal;
import com.turaf.experiment.application.ProblemService;
import com.turaf.experiment.application.dto.CreateProblemRequest;
import com.turaf.experiment.application.dto.ProblemDto;
import com.turaf.experiment.application.dto.UpdateProblemRequest;
import com.turaf.experiment.domain.ProblemId;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/problems")
@PreAuthorize("isAuthenticated()")
public class ProblemController {
    
    private final ProblemService problemService;

    public ProblemController(ProblemService problemService) {
        this.problemService = problemService;
    }

    @PostMapping
    public ResponseEntity<ProblemDto> createProblem(
            @Valid @RequestBody CreateProblemRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        ProblemDto problem = problemService.createProblem(request, principal.getUserId());
        return ResponseEntity.status(HttpStatus.CREATED).body(problem);
    }

    @GetMapping
    public ResponseEntity<List<ProblemDto>> getAllProblems() {
        List<ProblemDto> problems = problemService.getAllProblems();
        return ResponseEntity.ok(problems);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProblemDto> getProblem(@PathVariable String id) {
        ProblemDto problem = problemService.getProblem(ProblemId.of(id));
        return ResponseEntity.ok(problem);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProblemDto> updateProblem(
            @PathVariable String id,
            @Valid @RequestBody UpdateProblemRequest request) {
        ProblemDto problem = problemService.updateProblem(ProblemId.of(id), request);
        return ResponseEntity.ok(problem);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProblem(@PathVariable String id) {
        problemService.deleteProblem(ProblemId.of(id));
        return ResponseEntity.noContent().build();
    }
}
