# Task: Implement Experiment State Machine

**Service**: Experiment Service  
**Phase**: 4  
**Estimated Time**: 2-3 hours  

## Objective

Implement a robust state machine for experiment lifecycle management with validation and transition rules.

## Prerequisites

- [x] Task 001: Domain model created

## Scope

**Files to Create**:
- `services/experiment-service/src/main/java/com/turaf/experiment/domain/ExperimentStateMachine.java`
- `services/experiment-service/src/main/java/com/turaf/experiment/domain/StateTransition.java`
- `services/experiment-service/src/main/java/com/turaf/experiment/domain/exception/InvalidStateTransitionException.java`

## Implementation Details

### State Transition Value Object

```java
public class StateTransition extends ValueObject {
    private final ExperimentStatus from;
    private final ExperimentStatus to;
    
    public StateTransition(ExperimentStatus from, ExperimentStatus to) {
        this.from = Objects.requireNonNull(from);
        this.to = Objects.requireNonNull(to);
    }
    
    @Override
    protected List<Object> getEqualityComponents() {
        return List.of(from, to);
    }
    
    // Getters
}
```

### Experiment State Machine

```java
public class ExperimentStateMachine {
    private static final Set<StateTransition> VALID_TRANSITIONS = Set.of(
        new StateTransition(ExperimentStatus.DRAFT, ExperimentStatus.RUNNING),
        new StateTransition(ExperimentStatus.RUNNING, ExperimentStatus.COMPLETED),
        new StateTransition(ExperimentStatus.RUNNING, ExperimentStatus.CANCELLED),
        new StateTransition(ExperimentStatus.DRAFT, ExperimentStatus.CANCELLED)
    );
    
    public static void validateTransition(ExperimentStatus from, ExperimentStatus to) {
        StateTransition transition = new StateTransition(from, to);
        
        if (!VALID_TRANSITIONS.contains(transition)) {
            throw new InvalidStateTransitionException(
                String.format("Invalid state transition from %s to %s", from, to)
            );
        }
    }
    
    public static boolean canTransition(ExperimentStatus from, ExperimentStatus to) {
        StateTransition transition = new StateTransition(from, to);
        return VALID_TRANSITIONS.contains(transition);
    }
    
    public static Set<ExperimentStatus> getAllowedTransitions(ExperimentStatus current) {
        return VALID_TRANSITIONS.stream()
            .filter(t -> t.getFrom() == current)
            .map(StateTransition::getTo)
            .collect(Collectors.toSet());
    }
}
```

### Update Experiment Entity

```java
public class Experiment extends AggregateRoot<ExperimentId> implements TenantAware {
    // ... existing fields ...
    
    public void start() {
        ExperimentStateMachine.validateTransition(status, ExperimentStatus.RUNNING);
        this.status = ExperimentStatus.RUNNING;
        this.startedAt = Instant.now();
        this.updatedAt = Instant.now();
        
        registerEvent(new ExperimentStarted(
            UUID.randomUUID().toString(),
            getId().getValue(),
            organizationId,
            hypothesisId.getValue(),
            startedAt
        ));
    }
    
    public void complete() {
        ExperimentStateMachine.validateTransition(status, ExperimentStatus.COMPLETED);
        this.status = ExperimentStatus.COMPLETED;
        this.completedAt = Instant.now();
        this.updatedAt = Instant.now();
        
        registerEvent(new ExperimentCompleted(
            UUID.randomUUID().toString(),
            getId().getValue(),
            organizationId,
            hypothesisId.getValue(),
            completedAt
        ));
    }
    
    public void cancel() {
        ExperimentStateMachine.validateTransition(status, ExperimentStatus.CANCELLED);
        this.status = ExperimentStatus.CANCELLED;
        this.updatedAt = Instant.now();
    }
    
    public Set<ExperimentStatus> getAllowedTransitions() {
        return ExperimentStateMachine.getAllowedTransitions(status);
    }
}
```

## Acceptance Criteria

- [ ] State machine defines all valid transitions
- [ ] Invalid transitions throw exceptions
- [ ] State machine is immutable and thread-safe
- [ ] Experiment entity uses state machine for validation
- [ ] getAllowedTransitions returns correct states
- [ ] Unit tests pass

## Testing Requirements

**Unit Tests**:
- Test all valid state transitions
- Test all invalid state transitions throw exceptions
- Test getAllowedTransitions for each state
- Test canTransition method
- Test Experiment entity state transitions

**Test Files to Create**:
- `ExperimentStateMachineTest.java`
- `StateTransitionTest.java`

## References

- Specification: `specs/experiment-service.md` (State Machine section)
- Related Tasks: 001-create-domain-model, 006-implement-experiment-service
