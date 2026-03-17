package com.turaf.experiment.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class StateTransitionTest {

    @Test
    void shouldCreateStateTransitionWithValidStatuses() {
        // Given
        ExperimentStatus from = ExperimentStatus.DRAFT;
        ExperimentStatus to = ExperimentStatus.RUNNING;

        // When
        StateTransition transition = new StateTransition(from, to);

        // Then
        assertEquals(from, transition.getFrom());
        assertEquals(to, transition.getTo());
    }

    @Test
    void shouldThrowExceptionWhenFromStatusIsNull() {
        // When & Then
        assertThrows(NullPointerException.class, () -> {
            new StateTransition(null, ExperimentStatus.RUNNING);
        });
    }

    @Test
    void shouldThrowExceptionWhenToStatusIsNull() {
        // When & Then
        assertThrows(NullPointerException.class, () -> {
            new StateTransition(ExperimentStatus.DRAFT, null);
        });
    }

    @Test
    void shouldBeEqualWhenSameTransition() {
        // Given
        StateTransition transition1 = new StateTransition(ExperimentStatus.DRAFT, ExperimentStatus.RUNNING);
        StateTransition transition2 = new StateTransition(ExperimentStatus.DRAFT, ExperimentStatus.RUNNING);

        // When & Then
        assertEquals(transition1, transition2);
        assertEquals(transition1.hashCode(), transition2.hashCode());
    }

    @Test
    void shouldNotBeEqualWhenDifferentFromStatus() {
        // Given
        StateTransition transition1 = new StateTransition(ExperimentStatus.DRAFT, ExperimentStatus.RUNNING);
        StateTransition transition2 = new StateTransition(ExperimentStatus.RUNNING, ExperimentStatus.RUNNING);

        // When & Then
        assertNotEquals(transition1, transition2);
    }

    @Test
    void shouldNotBeEqualWhenDifferentToStatus() {
        // Given
        StateTransition transition1 = new StateTransition(ExperimentStatus.DRAFT, ExperimentStatus.RUNNING);
        StateTransition transition2 = new StateTransition(ExperimentStatus.DRAFT, ExperimentStatus.COMPLETED);

        // When & Then
        assertNotEquals(transition1, transition2);
    }

    @Test
    void shouldHaveReadableToString() {
        // Given
        StateTransition transition = new StateTransition(ExperimentStatus.DRAFT, ExperimentStatus.RUNNING);

        // When
        String result = transition.toString();

        // Then
        assertTrue(result.contains("DRAFT"));
        assertTrue(result.contains("RUNNING"));
        assertTrue(result.contains("->"));
    }

    @Test
    void shouldBeEqualToItself() {
        // Given
        StateTransition transition = new StateTransition(ExperimentStatus.DRAFT, ExperimentStatus.RUNNING);

        // When & Then
        assertEquals(transition, transition);
    }

    @Test
    void shouldNotBeEqualToNull() {
        // Given
        StateTransition transition = new StateTransition(ExperimentStatus.DRAFT, ExperimentStatus.RUNNING);

        // When & Then
        assertNotEquals(transition, null);
    }

    @Test
    void shouldNotBeEqualToDifferentClass() {
        // Given
        StateTransition transition = new StateTransition(ExperimentStatus.DRAFT, ExperimentStatus.RUNNING);

        // When & Then
        assertNotEquals(transition, "DRAFT -> RUNNING");
    }
}
