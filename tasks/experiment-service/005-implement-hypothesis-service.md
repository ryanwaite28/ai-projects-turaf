# Task: Implement Hypothesis Service

**Service**: Experiment Service  
**Phase**: 4  
**Estimated Time**: 2 hours  

## Objective

Implement the application layer service for hypothesis management including CRUD operations.

## Prerequisites

- [x] Task 001: Domain model created
- [x] Task 003: Repositories implemented
- [x] Task 004: Problem service implemented

## Scope

**Files to Create**:
- `services/experiment-service/src/main/java/com/turaf/experiment/application/HypothesisService.java`
- `services/experiment-service/src/main/java/com/turaf/experiment/application/dto/CreateHypothesisRequest.java`
- `services/experiment-service/src/main/java/com/turaf/experiment/application/dto/UpdateHypothesisRequest.java`
- `services/experiment-service/src/main/java/com/turaf/experiment/application/dto/HypothesisDto.java`

## Implementation Details

### Hypothesis Service

```java
@Service
@Transactional
public class HypothesisService {
    private final HypothesisRepository hypothesisRepository;
    private final ProblemRepository problemRepository;
    private final EventPublisher eventPublisher;
    
    public HypothesisDto createHypothesis(CreateHypothesisRequest request, String createdBy) {
        String organizationId = TenantContextHolder.getOrganizationId();
        ProblemId problemId = new ProblemId(request.getProblemId());
        
        // Verify problem exists
        problemRepository.findById(problemId)
            .orElseThrow(() -> new ProblemNotFoundException("Problem not found"));
        
        HypothesisId id = HypothesisId.generate();
        Hypothesis hypothesis = new Hypothesis(
            id,
            organizationId,
            problemId,
            request.getStatement(),
            request.getExpectedOutcome(),
            createdBy
        );
        
        Hypothesis saved = hypothesisRepository.save(hypothesis);
        
        saved.getDomainEvents().forEach(eventPublisher::publish);
        saved.clearDomainEvents();
        
        return HypothesisDto.fromDomain(saved);
    }
    
    public HypothesisDto getHypothesis(HypothesisId id) {
        Hypothesis hypothesis = hypothesisRepository.findById(id)
            .orElseThrow(() -> new HypothesisNotFoundException("Hypothesis not found"));
        return HypothesisDto.fromDomain(hypothesis);
    }
    
    public List<HypothesisDto> getHypothesesByProblem(ProblemId problemId) {
        return hypothesisRepository.findByProblemId(problemId)
            .stream()
            .map(HypothesisDto::fromDomain)
            .collect(Collectors.toList());
    }
    
    public HypothesisDto updateHypothesis(HypothesisId id, UpdateHypothesisRequest request) {
        Hypothesis hypothesis = hypothesisRepository.findById(id)
            .orElseThrow(() -> new HypothesisNotFoundException("Hypothesis not found"));
        
        hypothesis.update(request.getStatement(), request.getExpectedOutcome());
        Hypothesis updated = hypothesisRepository.save(hypothesis);
        
        return HypothesisDto.fromDomain(updated);
    }
    
    public void deleteHypothesis(HypothesisId id) {
        Hypothesis hypothesis = hypothesisRepository.findById(id)
            .orElseThrow(() -> new HypothesisNotFoundException("Hypothesis not found"));
        
        hypothesisRepository.delete(hypothesis);
    }
}
```

### DTOs

```java
public class CreateHypothesisRequest {
    @NotBlank
    private String problemId;
    
    @NotBlank
    @Size(min = 1, max = 500)
    private String statement;
    
    private String expectedOutcome;
    
    // Getters, setters
}

public class HypothesisDto {
    private String id;
    private String organizationId;
    private String problemId;
    private String statement;
    private String expectedOutcome;
    private String createdBy;
    private Instant createdAt;
    private Instant updatedAt;
    
    public static HypothesisDto fromDomain(Hypothesis hypothesis) {
        HypothesisDto dto = new HypothesisDto();
        dto.setId(hypothesis.getId().getValue());
        dto.setOrganizationId(hypothesis.getOrganizationId());
        dto.setProblemId(hypothesis.getProblemId().getValue());
        dto.setStatement(hypothesis.getStatement());
        dto.setExpectedOutcome(hypothesis.getExpectedOutcome());
        dto.setCreatedBy(hypothesis.getCreatedBy());
        dto.setCreatedAt(hypothesis.getCreatedAt());
        dto.setUpdatedAt(hypothesis.getUpdatedAt());
        return dto;
    }
}
```

## Acceptance Criteria

- [x] Create hypothesis validates problem exists
- [x] Get hypothesis by ID works
- [x] Get hypotheses by problem works
- [x] Update hypothesis works
- [x] Delete hypothesis works
- [x] Domain events published
- [x] Validation enforced
- [x] Unit tests pass

## Testing Requirements

**Unit Tests**:
- Test create hypothesis
- Test create with invalid problem ID
- Test get hypothesis
- Test get by problem
- Test update hypothesis
- Test delete hypothesis

**Test Files to Create**:
- `HypothesisServiceTest.java`

## References

- Specification: `specs/experiment-service.md` (Hypothesis Management section)
- Related Tasks: 006-implement-experiment-service
