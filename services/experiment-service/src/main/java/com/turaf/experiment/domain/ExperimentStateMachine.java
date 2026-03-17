package com.turaf.experiment.domain;

import com.turaf.experiment.domain.exception.InvalidStateTransitionException;

import java.util.Set;
import java.util.stream.Collectors;

public final class ExperimentStateMachine {
    
    private static final Set<StateTransition> VALID_TRANSITIONS = Set.of(
        new StateTransition(ExperimentStatus.DRAFT, ExperimentStatus.RUNNING),
        new StateTransition(ExperimentStatus.RUNNING, ExperimentStatus.COMPLETED),
        new StateTransition(ExperimentStatus.RUNNING, ExperimentStatus.CANCELLED),
        new StateTransition(ExperimentStatus.DRAFT, ExperimentStatus.CANCELLED)
    );

    private ExperimentStateMachine() {
        // Prevent instantiation
    }

    public static void validateTransition(ExperimentStatus from, ExperimentStatus to) {
        if (from == null) {
            throw new IllegalArgumentException("Current status cannot be null");
        }
        if (to == null) {
            throw new IllegalArgumentException("Target status cannot be null");
        }
        
        StateTransition transition = new StateTransition(from, to);
        
        if (!VALID_TRANSITIONS.contains(transition)) {
            throw new InvalidStateTransitionException(
                String.format("Invalid state transition from %s to %s", from, to)
            );
        }
    }

    public static boolean canTransition(ExperimentStatus from, ExperimentStatus to) {
        if (from == null || to == null) {
            return false;
        }
        
        StateTransition transition = new StateTransition(from, to);
        return VALID_TRANSITIONS.contains(transition);
    }

    public static Set<ExperimentStatus> getAllowedTransitions(ExperimentStatus current) {
        if (current == null) {
            return Set.of();
        }
        
        return VALID_TRANSITIONS.stream()
            .filter(t -> t.getFrom() == current)
            .map(StateTransition::getTo)
            .collect(Collectors.toUnmodifiableSet());
    }

    public static Set<StateTransition> getAllValidTransitions() {
        return Set.copyOf(VALID_TRANSITIONS);
    }
}
