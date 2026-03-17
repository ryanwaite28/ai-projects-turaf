package com.turaf.experiment.domain;

import com.turaf.experiment.domain.exception.InvalidStateTransitionException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ExperimentStateMachineTest {

    // ========== ExperimentStateMachine Class Tests ==========

    @Test
    void shouldValidateValidTransitionFromDraftToRunning() {
        // When & Then
        assertDoesNotThrow(() -> {
            ExperimentStateMachine.validateTransition(ExperimentStatus.DRAFT, ExperimentStatus.RUNNING);
        });
    }

    @Test
    void shouldValidateValidTransitionFromRunningToCompleted() {
        // When & Then
        assertDoesNotThrow(() -> {
            ExperimentStateMachine.validateTransition(ExperimentStatus.RUNNING, ExperimentStatus.COMPLETED);
        });
    }

    @Test
    void shouldValidateValidTransitionFromRunningToCancelled() {
        // When & Then
        assertDoesNotThrow(() -> {
            ExperimentStateMachine.validateTransition(ExperimentStatus.RUNNING, ExperimentStatus.CANCELLED);
        });
    }

    @Test
    void shouldValidateValidTransitionFromDraftToCancelled() {
        // When & Then
        assertDoesNotThrow(() -> {
            ExperimentStateMachine.validateTransition(ExperimentStatus.DRAFT, ExperimentStatus.CANCELLED);
        });
    }

    @Test
    void shouldThrowExceptionForInvalidTransitionFromDraftToCompleted() {
        // When & Then
        InvalidStateTransitionException exception = assertThrows(InvalidStateTransitionException.class, () -> {
            ExperimentStateMachine.validateTransition(ExperimentStatus.DRAFT, ExperimentStatus.COMPLETED);
        });
        assertTrue(exception.getMessage().contains("DRAFT"));
        assertTrue(exception.getMessage().contains("COMPLETED"));
    }

    @Test
    void shouldThrowExceptionForInvalidTransitionFromCompletedToRunning() {
        // When & Then
        assertThrows(InvalidStateTransitionException.class, () -> {
            ExperimentStateMachine.validateTransition(ExperimentStatus.COMPLETED, ExperimentStatus.RUNNING);
        });
    }

    @Test
    void shouldThrowExceptionForInvalidTransitionFromCancelledToRunning() {
        // When & Then
        assertThrows(InvalidStateTransitionException.class, () -> {
            ExperimentStateMachine.validateTransition(ExperimentStatus.CANCELLED, ExperimentStatus.RUNNING);
        });
    }

    @Test
    void shouldThrowExceptionWhenFromStatusIsNull() {
        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            ExperimentStateMachine.validateTransition(null, ExperimentStatus.RUNNING);
        });
    }

    @Test
    void shouldThrowExceptionWhenToStatusIsNull() {
        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            ExperimentStateMachine.validateTransition(ExperimentStatus.DRAFT, null);
        });
    }

    @Test
    void shouldReturnTrueForValidTransition() {
        // When
        boolean canTransition = ExperimentStateMachine.canTransition(ExperimentStatus.DRAFT, ExperimentStatus.RUNNING);

        // Then
        assertTrue(canTransition);
    }

    @Test
    void shouldReturnFalseForInvalidTransition() {
        // When
        boolean canTransition = ExperimentStateMachine.canTransition(ExperimentStatus.DRAFT, ExperimentStatus.COMPLETED);

        // Then
        assertFalse(canTransition);
    }

    @Test
    void shouldReturnFalseWhenFromStatusIsNull() {
        // When
        boolean canTransition = ExperimentStateMachine.canTransition(null, ExperimentStatus.RUNNING);

        // Then
        assertFalse(canTransition);
    }

    @Test
    void shouldReturnFalseWhenToStatusIsNull() {
        // When
        boolean canTransition = ExperimentStateMachine.canTransition(ExperimentStatus.DRAFT, null);

        // Then
        assertFalse(canTransition);
    }

    @Test
    void shouldReturnAllowedTransitionsFromDraft() {
        // When
        Set<ExperimentStatus> allowedTransitions = ExperimentStateMachine.getAllowedTransitions(ExperimentStatus.DRAFT);

        // Then
        assertEquals(2, allowedTransitions.size());
        assertTrue(allowedTransitions.contains(ExperimentStatus.RUNNING));
        assertTrue(allowedTransitions.contains(ExperimentStatus.CANCELLED));
    }

    @Test
    void shouldReturnAllowedTransitionsFromRunning() {
        // When
        Set<ExperimentStatus> allowedTransitions = ExperimentStateMachine.getAllowedTransitions(ExperimentStatus.RUNNING);

        // Then
        assertEquals(2, allowedTransitions.size());
        assertTrue(allowedTransitions.contains(ExperimentStatus.COMPLETED));
        assertTrue(allowedTransitions.contains(ExperimentStatus.CANCELLED));
    }

    @Test
    void shouldReturnEmptySetForCompletedStatus() {
        // When
        Set<ExperimentStatus> allowedTransitions = ExperimentStateMachine.getAllowedTransitions(ExperimentStatus.COMPLETED);

        // Then
        assertTrue(allowedTransitions.isEmpty());
    }

    @Test
    void shouldReturnEmptySetForCancelledStatus() {
        // When
        Set<ExperimentStatus> allowedTransitions = ExperimentStateMachine.getAllowedTransitions(ExperimentStatus.CANCELLED);

        // Then
        assertTrue(allowedTransitions.isEmpty());
    }

    @Test
    void shouldReturnEmptySetWhenCurrentStatusIsNull() {
        // When
        Set<ExperimentStatus> allowedTransitions = ExperimentStateMachine.getAllowedTransitions(null);

        // Then
        assertTrue(allowedTransitions.isEmpty());
    }

    @Test
    void shouldReturnImmutableSetOfAllowedTransitions() {
        // Given
        Set<ExperimentStatus> allowedTransitions = ExperimentStateMachine.getAllowedTransitions(ExperimentStatus.DRAFT);

        // When & Then
        assertThrows(UnsupportedOperationException.class, () -> {
            allowedTransitions.add(ExperimentStatus.COMPLETED);
        });
    }

    @Test
    void shouldReturnAllValidTransitions() {
        // When
        Set<StateTransition> validTransitions = ExperimentStateMachine.getAllValidTransitions();

        // Then
        assertEquals(4, validTransitions.size());
        assertTrue(validTransitions.contains(new StateTransition(ExperimentStatus.DRAFT, ExperimentStatus.RUNNING)));
        assertTrue(validTransitions.contains(new StateTransition(ExperimentStatus.RUNNING, ExperimentStatus.COMPLETED)));
        assertTrue(validTransitions.contains(new StateTransition(ExperimentStatus.RUNNING, ExperimentStatus.CANCELLED)));
        assertTrue(validTransitions.contains(new StateTransition(ExperimentStatus.DRAFT, ExperimentStatus.CANCELLED)));
    }

    // ========== Experiment Entity State Machine Integration Tests ==========

    @Test
    void shouldFollowValidStateTransitionFromDraftToRunning() {
        // Given
        Experiment experiment = createExperiment();
        assertEquals(ExperimentStatus.DRAFT, experiment.getStatus());

        // When
        experiment.start();

        // Then
        assertEquals(ExperimentStatus.RUNNING, experiment.getStatus());
    }

    @Test
    void shouldFollowValidStateTransitionFromRunningToCompleted() {
        // Given
        Experiment experiment = createExperiment();
        experiment.start();
        assertEquals(ExperimentStatus.RUNNING, experiment.getStatus());

        // When
        experiment.complete();

        // Then
        assertEquals(ExperimentStatus.COMPLETED, experiment.getStatus());
    }

    @Test
    void shouldFollowValidStateTransitionFromDraftToCancelled() {
        // Given
        Experiment experiment = createExperiment();
        assertEquals(ExperimentStatus.DRAFT, experiment.getStatus());

        // When
        experiment.cancel();

        // Then
        assertEquals(ExperimentStatus.CANCELLED, experiment.getStatus());
    }

    @Test
    void shouldFollowValidStateTransitionFromRunningToCancelled() {
        // Given
        Experiment experiment = createExperiment();
        experiment.start();
        assertEquals(ExperimentStatus.RUNNING, experiment.getStatus());

        // When
        experiment.cancel();

        // Then
        assertEquals(ExperimentStatus.CANCELLED, experiment.getStatus());
    }

    @Test
    void shouldRejectStartFromRunningStatus() {
        // Given
        Experiment experiment = createExperiment();
        experiment.start();

        // When & Then
        assertThrows(IllegalStateException.class, experiment::start);
    }

    @Test
    void shouldRejectStartFromCompletedStatus() {
        // Given
        Experiment experiment = createExperiment();
        experiment.start();
        experiment.complete();

        // When & Then
        assertThrows(IllegalStateException.class, experiment::start);
    }

    @Test
    void shouldRejectStartFromCancelledStatus() {
        // Given
        Experiment experiment = createExperiment();
        experiment.cancel();

        // When & Then
        assertThrows(IllegalStateException.class, experiment::start);
    }

    @Test
    void shouldRejectCompleteFromDraftStatus() {
        // Given
        Experiment experiment = createExperiment();

        // When & Then
        assertThrows(IllegalStateException.class, experiment::complete);
    }

    @Test
    void shouldRejectCompleteFromCompletedStatus() {
        // Given
        Experiment experiment = createExperiment();
        experiment.start();
        experiment.complete();

        // When & Then
        assertThrows(IllegalStateException.class, experiment::complete);
    }

    @Test
    void shouldRejectCompleteFromCancelledStatus() {
        // Given
        Experiment experiment = createExperiment();
        experiment.cancel();

        // When & Then
        assertThrows(IllegalStateException.class, experiment::complete);
    }

    @Test
    void shouldRejectCancelFromCompletedStatus() {
        // Given
        Experiment experiment = createExperiment();
        experiment.start();
        experiment.complete();

        // When & Then
        assertThrows(IllegalStateException.class, experiment::cancel);
    }

    @Test
    void shouldRejectCancelFromCancelledStatus() {
        // Given
        Experiment experiment = createExperiment();
        experiment.cancel();

        // When & Then
        assertThrows(IllegalStateException.class, experiment::cancel);
    }

    @Test
    void shouldOnlyAllowUpdateInDraftStatus() {
        // Given
        Experiment experiment = createExperiment();

        // When & Then - DRAFT status should allow update
        assertDoesNotThrow(() -> experiment.update("New Name", "New Description"));
    }

    @ParameterizedTest
    @EnumSource(value = ExperimentStatus.class, names = {"RUNNING", "COMPLETED", "CANCELLED"})
    void shouldRejectUpdateInNonDraftStatus(ExperimentStatus status) {
        // Given
        Experiment experiment = createExperiment();
        
        // Transition to target status
        switch (status) {
            case RUNNING:
                experiment.start();
                break;
            case COMPLETED:
                experiment.start();
                experiment.complete();
                break;
            case CANCELLED:
                experiment.cancel();
                break;
        }

        // When & Then
        assertThrows(IllegalStateException.class, () -> {
            experiment.update("New Name", "New Description");
        });
    }

    @Test
    void shouldMaintainStateConsistencyThroughCompleteFlow() {
        // Given
        Experiment experiment = createExperiment();

        // When & Then - Complete flow: DRAFT -> RUNNING -> COMPLETED
        assertEquals(ExperimentStatus.DRAFT, experiment.getStatus());
        assertNull(experiment.getStartedAt());
        assertNull(experiment.getCompletedAt());

        experiment.start();
        assertEquals(ExperimentStatus.RUNNING, experiment.getStatus());
        assertNotNull(experiment.getStartedAt());
        assertNull(experiment.getCompletedAt());

        experiment.complete();
        assertEquals(ExperimentStatus.COMPLETED, experiment.getStatus());
        assertNotNull(experiment.getStartedAt());
        assertNotNull(experiment.getCompletedAt());
    }

    @Test
    void shouldMaintainStateConsistencyThroughCancelFlow() {
        // Given
        Experiment experiment = createExperiment();

        // When & Then - Cancel flow: DRAFT -> RUNNING -> CANCELLED
        assertEquals(ExperimentStatus.DRAFT, experiment.getStatus());

        experiment.start();
        assertEquals(ExperimentStatus.RUNNING, experiment.getStatus());

        experiment.cancel();
        assertEquals(ExperimentStatus.CANCELLED, experiment.getStatus());
    }

    @Test
    void shouldReturnAllowedTransitionsForExperimentInDraftStatus() {
        // Given
        Experiment experiment = createExperiment();

        // When
        Set<ExperimentStatus> allowedTransitions = experiment.getAllowedTransitions();

        // Then
        assertEquals(2, allowedTransitions.size());
        assertTrue(allowedTransitions.contains(ExperimentStatus.RUNNING));
        assertTrue(allowedTransitions.contains(ExperimentStatus.CANCELLED));
    }

    @Test
    void shouldReturnAllowedTransitionsForExperimentInRunningStatus() {
        // Given
        Experiment experiment = createExperiment();
        experiment.start();

        // When
        Set<ExperimentStatus> allowedTransitions = experiment.getAllowedTransitions();

        // Then
        assertEquals(2, allowedTransitions.size());
        assertTrue(allowedTransitions.contains(ExperimentStatus.COMPLETED));
        assertTrue(allowedTransitions.contains(ExperimentStatus.CANCELLED));
    }

    @Test
    void shouldReturnEmptyAllowedTransitionsForCompletedExperiment() {
        // Given
        Experiment experiment = createExperiment();
        experiment.start();
        experiment.complete();

        // When
        Set<ExperimentStatus> allowedTransitions = experiment.getAllowedTransitions();

        // Then
        assertTrue(allowedTransitions.isEmpty());
    }

    @Test
    void shouldCheckIfExperimentCanTransitionToTargetStatus() {
        // Given
        Experiment experiment = createExperiment();

        // When & Then
        assertTrue(experiment.canTransitionTo(ExperimentStatus.RUNNING));
        assertTrue(experiment.canTransitionTo(ExperimentStatus.CANCELLED));
        assertFalse(experiment.canTransitionTo(ExperimentStatus.COMPLETED));
    }

    @Test
    void shouldCheckIfRunningExperimentCanTransitionToTargetStatus() {
        // Given
        Experiment experiment = createExperiment();
        experiment.start();

        // When & Then
        assertFalse(experiment.canTransitionTo(ExperimentStatus.RUNNING));
        assertTrue(experiment.canTransitionTo(ExperimentStatus.COMPLETED));
        assertTrue(experiment.canTransitionTo(ExperimentStatus.CANCELLED));
    }

    private Experiment createExperiment() {
        return new Experiment(
            ExperimentId.generate(),
            "org-123",
            HypothesisId.generate(),
            "Test Experiment",
            "Test Description",
            "user-456"
        );
    }
}
