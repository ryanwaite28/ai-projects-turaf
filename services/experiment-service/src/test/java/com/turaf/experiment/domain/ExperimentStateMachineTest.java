package com.turaf.experiment.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.*;

class ExperimentStateMachineTest {

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
