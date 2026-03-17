# Task: Implement Experiment Service

**Service**: Experiment Service  
**Phase**: 4  
**Estimated Time**: 3 hours  

## Objective

Implement the application layer service for experiment management including CRUD operations and state transitions.

## Prerequisites

- [x] Task 001: Domain model created
- [x] Task 002: State machine implemented
- [x] Task 003: Repositories implemented
- [x] Task 005: Hypothesis service implemented

## Scope

**Files to Create**:
- `services/experiment-service/src/main/java/com/turaf/experiment/application/ExperimentService.java`
- `services/experiment-service/src/main/java/com/turaf/experiment/application/dto/CreateExperimentRequest.java`
- `services/experiment-service/src/main/java/com/turaf/experiment/application/dto/UpdateExperimentRequest.java`
- `services/experiment-service/src/main/java/com/turaf/experiment/application/dto/ExperimentDto.java`

## Implementation Details

### Experiment Service

```java
@Service
@Transactional
public class ExperimentService {
    private final ExperimentRepository experimentRepository;
    private final HypothesisRepository hypothesisRepository;
    private final EventPublisher eventPublisher;
    
    public ExperimentDto createExperiment(CreateExperimentRequest request, String createdBy) {
        String organizationId = TenantContextHolder.getOrganizationId();
        HypothesisId hypothesisId = new HypothesisId(request.getHypothesisId());
        
        // Verify hypothesis exists
        hypothesisRepository.findById(hypothesisId)
            .orElseThrow(() -> new HypothesisNotFoundException("Hypothesis not found"));
        
        ExperimentId id = ExperimentId.generate();
        Experiment experiment = new Experiment(
            id,
            organizationId,
            hypothesisId,
            request.getName(),
            request.getDescription(),
            createdBy
        );
        
        Experiment saved = experimentRepository.save(experiment);
        
        saved.getDomainEvents().forEach(eventPublisher::publish);
        saved.clearDomainEvents();
        
        return ExperimentDto.fromDomain(saved);
    }
    
    public ExperimentDto getExperiment(ExperimentId id) {
        Experiment experiment = experimentRepository.findById(id)
            .orElseThrow(() -> new ExperimentNotFoundException("Experiment not found"));
        return ExperimentDto.fromDomain(experiment);
    }
    
    public List<ExperimentDto> getExperimentsByHypothesis(HypothesisId hypothesisId) {
        return experimentRepository.findByHypothesisId(hypothesisId)
            .stream()
            .map(ExperimentDto::fromDomain)
            .collect(Collectors.toList());
    }
    
    public List<ExperimentDto> getExperimentsByStatus(ExperimentStatus status) {
        return experimentRepository.findByStatus(status)
            .stream()
            .map(ExperimentDto::fromDomain)
            .collect(Collectors.toList());
    }
    
    public ExperimentDto updateExperiment(ExperimentId id, UpdateExperimentRequest request) {
        Experiment experiment = experimentRepository.findById(id)
            .orElseThrow(() -> new ExperimentNotFoundException("Experiment not found"));
        
        experiment.update(request.getName(), request.getDescription());
        Experiment updated = experimentRepository.save(experiment);
        
        return ExperimentDto.fromDomain(updated);
    }
    
    public ExperimentDto startExperiment(ExperimentId id) {
        Experiment experiment = experimentRepository.findById(id)
            .orElseThrow(() -> new ExperimentNotFoundException("Experiment not found"));
        
        experiment.start();
        Experiment updated = experimentRepository.save(experiment);
        
        updated.getDomainEvents().forEach(eventPublisher::publish);
        updated.clearDomainEvents();
        
        return ExperimentDto.fromDomain(updated);
    }
    
    public ExperimentDto completeExperiment(ExperimentId id) {
        Experiment experiment = experimentRepository.findById(id)
            .orElseThrow(() -> new ExperimentNotFoundException("Experiment not found"));
        
        experiment.complete();
        Experiment updated = experimentRepository.save(experiment);
        
        updated.getDomainEvents().forEach(eventPublisher::publish);
        updated.clearDomainEvents();
        
        return ExperimentDto.fromDomain(updated);
    }
    
    public void deleteExperiment(ExperimentId id) {
        Experiment experiment = experimentRepository.findById(id)
            .orElseThrow(() -> new ExperimentNotFoundException("Experiment not found"));
        
        experimentRepository.delete(experiment);
    }
}
```

### DTOs

```java
public class CreateExperimentRequest {
    @NotBlank
    private String hypothesisId;
    
    @NotBlank
    @Size(min = 1, max = 200)
    private String name;
    
    private String description;
    
    // Getters, setters
}

public class ExperimentDto {
    private String id;
    private String organizationId;
    private String hypothesisId;
    private String name;
    private String description;
    private String status;
    private Instant startedAt;
    private Instant completedAt;
    private String createdBy;
    private Instant createdAt;
    private Instant updatedAt;
    private Set<String> allowedTransitions;
    
    public static ExperimentDto fromDomain(Experiment experiment) {
        ExperimentDto dto = new ExperimentDto();
        dto.setId(experiment.getId().getValue());
        dto.setOrganizationId(experiment.getOrganizationId());
        dto.setHypothesisId(experiment.getHypothesisId().getValue());
        dto.setName(experiment.getName());
        dto.setDescription(experiment.getDescription());
        dto.setStatus(experiment.getStatus().name());
        dto.setStartedAt(experiment.getStartedAt());
        dto.setCompletedAt(experiment.getCompletedAt());
        dto.setCreatedBy(experiment.getCreatedBy());
        dto.setCreatedAt(experiment.getCreatedAt());
        dto.setUpdatedAt(experiment.getUpdatedAt());
        dto.setAllowedTransitions(
            experiment.getAllowedTransitions()
                .stream()
                .map(Enum::name)
                .collect(Collectors.toSet())
        );
        return dto;
    }
}
```

## Acceptance Criteria

- [ ] Create experiment validates hypothesis exists
- [ ] Get experiment by ID works
- [ ] Get experiments by hypothesis works
- [ ] Get experiments by status works
- [ ] Update experiment works
- [ ] Start experiment transitions state correctly
- [ ] Complete experiment transitions state correctly
- [ ] Invalid state transitions throw exceptions
- [ ] Domain events published
- [ ] Unit tests pass

## Testing Requirements

**Unit Tests**:
- Test create experiment
- Test create with invalid hypothesis
- Test get experiment
- Test start experiment
- Test complete experiment
- Test invalid state transitions
- Test event publishing

**Test Files to Create**:
- `ExperimentServiceTest.java`

## References

- Specification: `specs/experiment-service.md` (Experiment Management section)
- Related Tasks: 007-implement-rest-controllers
