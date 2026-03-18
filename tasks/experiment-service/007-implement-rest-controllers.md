# Task: Implement REST Controllers

**Service**: Experiment Service  
**Phase**: 4  
**Estimated Time**: 3 hours  

## Objective

Implement REST API endpoints for problem, hypothesis, and experiment management operations.

## Prerequisites

- [x] Task 004: Problem service implemented
- [x] Task 005: Hypothesis service implemented
- [x] Task 006: Experiment service implemented

## Scope

**Files to Create**:
- `services/experiment-service/src/main/java/com/turaf/experiment/interfaces/rest/ProblemController.java`
- `services/experiment-service/src/main/java/com/turaf/experiment/interfaces/rest/HypothesisController.java`
- `services/experiment-service/src/main/java/com/turaf/experiment/interfaces/rest/ExperimentController.java`
- `services/experiment-service/src/main/java/com/turaf/experiment/interfaces/rest/GlobalExceptionHandler.java`

## Implementation Details

### Problem Controller

```java
@RestController
@RequestMapping("/api/v1/problems")
@PreAuthorize("isAuthenticated()")
public class ProblemController {
    private final ProblemService problemService;
    
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
        ProblemDto problem = problemService.getProblem(new ProblemId(id));
        return ResponseEntity.ok(problem);
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<ProblemDto> updateProblem(
            @PathVariable String id,
            @Valid @RequestBody UpdateProblemRequest request) {
        ProblemDto problem = problemService.updateProblem(new ProblemId(id), request);
        return ResponseEntity.ok(problem);
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProblem(@PathVariable String id) {
        problemService.deleteProblem(new ProblemId(id));
        return ResponseEntity.noContent().build();
    }
}
```

### Experiment Controller

```java
@RestController
@RequestMapping("/api/v1/experiments")
@PreAuthorize("isAuthenticated()")
public class ExperimentController {
    private final ExperimentService experimentService;
    
    @PostMapping
    public ResponseEntity<ExperimentDto> createExperiment(
            @Valid @RequestBody CreateExperimentRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        ExperimentDto experiment = experimentService.createExperiment(request, principal.getUserId());
        return ResponseEntity.status(HttpStatus.CREATED).body(experiment);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<ExperimentDto> getExperiment(@PathVariable String id) {
        ExperimentDto experiment = experimentService.getExperiment(new ExperimentId(id));
        return ResponseEntity.ok(experiment);
    }
    
    @GetMapping
    public ResponseEntity<List<ExperimentDto>> getExperiments(
            @RequestParam(required = false) String hypothesisId,
            @RequestParam(required = false) String status) {
        
        if (hypothesisId != null) {
            List<ExperimentDto> experiments = experimentService.getExperimentsByHypothesis(
                new HypothesisId(hypothesisId)
            );
            return ResponseEntity.ok(experiments);
        }
        
        if (status != null) {
            List<ExperimentDto> experiments = experimentService.getExperimentsByStatus(
                ExperimentStatus.valueOf(status)
            );
            return ResponseEntity.ok(experiments);
        }
        
        return ResponseEntity.badRequest().build();
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<ExperimentDto> updateExperiment(
            @PathVariable String id,
            @Valid @RequestBody UpdateExperimentRequest request) {
        ExperimentDto experiment = experimentService.updateExperiment(new ExperimentId(id), request);
        return ResponseEntity.ok(experiment);
    }
    
    @PostMapping("/{id}/start")
    public ResponseEntity<ExperimentDto> startExperiment(@PathVariable String id) {
        ExperimentDto experiment = experimentService.startExperiment(new ExperimentId(id));
        return ResponseEntity.ok(experiment);
    }
    
    @PostMapping("/{id}/complete")
    public ResponseEntity<ExperimentDto> completeExperiment(@PathVariable String id) {
        ExperimentDto experiment = experimentService.completeExperiment(new ExperimentId(id));
        return ResponseEntity.ok(experiment);
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteExperiment(@PathVariable String id) {
        experimentService.deleteExperiment(new ExperimentId(id));
        return ResponseEntity.noContent().build();
    }
}
```

### Global Exception Handler

```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(ProblemNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleProblemNotFound(ProblemNotFoundException ex) {
        ErrorResponse error = new ErrorResponse("PROBLEM_NOT_FOUND", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }
    
    @ExceptionHandler(InvalidStateTransitionException.class)
    public ResponseEntity<ErrorResponse> handleInvalidStateTransition(InvalidStateTransitionException ex) {
        ErrorResponse error = new ErrorResponse("INVALID_STATE_TRANSITION", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }
}
```

## Acceptance Criteria

- [x] All problem endpoints work correctly
- [x] All hypothesis endpoints work correctly
- [x] All experiment endpoints work correctly
- [x] State transition endpoints work
- [x] Query parameters for filtering work
- [x] Validation enforced
- [x] Error handling works
- [ ] All endpoints documented with OpenAPI

## Testing Requirements

**Integration Tests**:
- Test all CRUD operations
- Test state transitions
- Test filtering by status
- Test error scenarios

**Test Files to Create**:
- `ProblemControllerTest.java`
- `HypothesisControllerTest.java`
- `ExperimentControllerTest.java`

## References

- Specification: `specs/experiment-service.md` (API Endpoints section)
- Related Tasks: 008-implement-event-publishing
