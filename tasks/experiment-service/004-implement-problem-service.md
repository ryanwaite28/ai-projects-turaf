# Task: Implement Problem Service

**Service**: Experiment Service  
**Phase**: 4  
**Estimated Time**: 2 hours  

## Objective

Implement the application layer service for problem management including CRUD operations.

## Prerequisites

- [x] Task 001: Domain model created
- [x] Task 003: Repositories implemented

## Scope

**Files to Create**:
- `services/experiment-service/src/main/java/com/turaf/experiment/application/ProblemService.java`
- `services/experiment-service/src/main/java/com/turaf/experiment/application/dto/CreateProblemRequest.java`
- `services/experiment-service/src/main/java/com/turaf/experiment/application/dto/UpdateProblemRequest.java`
- `services/experiment-service/src/main/java/com/turaf/experiment/application/dto/ProblemDto.java`

## Implementation Details

### Problem Service

```java
@Service
@Transactional
public class ProblemService {
    private final ProblemRepository problemRepository;
    private final EventPublisher eventPublisher;
    
    public ProblemDto createProblem(CreateProblemRequest request, String createdBy) {
        String organizationId = TenantContextHolder.getOrganizationId();
        ProblemId id = ProblemId.generate();
        
        Problem problem = new Problem(
            id,
            organizationId,
            request.getTitle(),
            request.getDescription(),
            createdBy
        );
        
        Problem saved = problemRepository.save(problem);
        
        saved.getDomainEvents().forEach(eventPublisher::publish);
        saved.clearDomainEvents();
        
        return ProblemDto.fromDomain(saved);
    }
    
    public ProblemDto getProblem(ProblemId id) {
        Problem problem = problemRepository.findById(id)
            .orElseThrow(() -> new ProblemNotFoundException("Problem not found"));
        return ProblemDto.fromDomain(problem);
    }
    
    public List<ProblemDto> getAllProblems() {
        String organizationId = TenantContextHolder.getOrganizationId();
        return problemRepository.findByOrganizationId(organizationId)
            .stream()
            .map(ProblemDto::fromDomain)
            .collect(Collectors.toList());
    }
    
    public ProblemDto updateProblem(ProblemId id, UpdateProblemRequest request) {
        Problem problem = problemRepository.findById(id)
            .orElseThrow(() -> new ProblemNotFoundException("Problem not found"));
        
        problem.update(request.getTitle(), request.getDescription());
        Problem updated = problemRepository.save(problem);
        
        return ProblemDto.fromDomain(updated);
    }
    
    public void deleteProblem(ProblemId id) {
        Problem problem = problemRepository.findById(id)
            .orElseThrow(() -> new ProblemNotFoundException("Problem not found"));
        
        problemRepository.delete(problem);
    }
}
```

### DTOs

```java
public class CreateProblemRequest {
    @NotBlank
    @Size(min = 1, max = 200)
    private String title;
    
    private String description;
    
    // Getters, setters
}

public class ProblemDto {
    private String id;
    private String organizationId;
    private String title;
    private String description;
    private String createdBy;
    private Instant createdAt;
    private Instant updatedAt;
    
    public static ProblemDto fromDomain(Problem problem) {
        ProblemDto dto = new ProblemDto();
        dto.setId(problem.getId().getValue());
        dto.setOrganizationId(problem.getOrganizationId());
        dto.setTitle(problem.getTitle());
        dto.setDescription(problem.getDescription());
        dto.setCreatedBy(problem.getCreatedBy());
        dto.setCreatedAt(problem.getCreatedAt());
        dto.setUpdatedAt(problem.getUpdatedAt());
        return dto;
    }
}
```

## Acceptance Criteria

- [x] Create problem works
- [x] Get problem by ID works
- [x] Get all problems scoped to organization
- [x] Update problem works
- [x] Delete problem works
- [x] Domain events published
- [x] Validation enforced
- [x] Unit tests pass

## Testing Requirements

**Unit Tests**:
- Test create problem
- Test get problem
- Test get all problems
- Test update problem
- Test delete problem
- Test event publishing

**Test Files to Create**:
- `ProblemServiceTest.java`

## References

- Specification: `specs/experiment-service.md` (Problem Management section)
- Related Tasks: 005-implement-hypothesis-service
