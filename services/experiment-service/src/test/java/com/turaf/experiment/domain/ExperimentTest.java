package com.turaf.experiment.domain;

import com.turaf.experiment.domain.event.*;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ExperimentTest {

    @Test
    void shouldCreateExperimentInDraftStatus() {
        // Given
        ExperimentId id = ExperimentId.generate();
        String organizationId = "org-123";
        HypothesisId hypothesisId = HypothesisId.generate();
        String name = "Cache Implementation Experiment";
        String description = "Test Redis caching for authentication";
        String createdBy = "user-456";

        // When
        Experiment experiment = new Experiment(id, organizationId, hypothesisId, name, description, createdBy);

        // Then
        assertEquals(id, experiment.getId());
        assertEquals(organizationId, experiment.getOrganizationId());
        assertEquals(hypothesisId, experiment.getHypothesisId());
        assertEquals(name, experiment.getName());
        assertEquals(description, experiment.getDescription());
        assertEquals(ExperimentStatus.DRAFT, experiment.getStatus());
        assertEquals(createdBy, experiment.getCreatedBy());
        assertNotNull(experiment.getCreatedAt());
        assertNotNull(experiment.getUpdatedAt());
        assertNull(experiment.getStartedAt());
        assertNull(experiment.getCompletedAt());
    }

    @Test
    void shouldRegisterExperimentCreatedEvent() {
        // Given
        ExperimentId id = ExperimentId.generate();
        String organizationId = "org-123";
        HypothesisId hypothesisId = HypothesisId.generate();

        // When
        Experiment experiment = new Experiment(id, organizationId, hypothesisId, "Name", "Description", "user-456");

        // Then
        assertEquals(1, experiment.getDomainEvents().size());
        assertTrue(experiment.getDomainEvents().get(0) instanceof ExperimentCreated);
        
        ExperimentCreated event = (ExperimentCreated) experiment.getDomainEvents().get(0);
        assertEquals(id.getValue(), event.getExperimentId());
        assertEquals(organizationId, event.getOrganizationId());
        assertEquals(hypothesisId.getValue(), event.getHypothesisId());
    }

    @Test
    void shouldStartExperimentFromDraftStatus() {
        // Given
        Experiment experiment = new Experiment(
            ExperimentId.generate(),
            "org-123",
            HypothesisId.generate(),
            "Test Experiment",
            "Description",
            "user-456"
        );

        // When
        experiment.start();

        // Then
        assertEquals(ExperimentStatus.RUNNING, experiment.getStatus());
        assertNotNull(experiment.getStartedAt());
        assertEquals(2, experiment.getDomainEvents().size());
        assertTrue(experiment.getDomainEvents().get(1) instanceof ExperimentStarted);
    }

    @Test
    void shouldCompleteExperimentFromRunningStatus() {
        // Given
        Experiment experiment = new Experiment(
            ExperimentId.generate(),
            "org-123",
            HypothesisId.generate(),
            "Test Experiment",
            "Description",
            "user-456"
        );
        experiment.start();

        // When
        experiment.complete();

        // Then
        assertEquals(ExperimentStatus.COMPLETED, experiment.getStatus());
        assertNotNull(experiment.getCompletedAt());
        assertEquals(3, experiment.getDomainEvents().size());
        assertTrue(experiment.getDomainEvents().get(2) instanceof ExperimentCompleted);
    }

    @Test
    void shouldCancelExperimentFromDraftStatus() {
        // Given
        Experiment experiment = new Experiment(
            ExperimentId.generate(),
            "org-123",
            HypothesisId.generate(),
            "Test Experiment",
            "Description",
            "user-456"
        );

        // When
        experiment.cancel();

        // Then
        assertEquals(ExperimentStatus.CANCELLED, experiment.getStatus());
        assertEquals(2, experiment.getDomainEvents().size());
        assertTrue(experiment.getDomainEvents().get(1) instanceof ExperimentCancelled);
    }

    @Test
    void shouldCancelExperimentFromRunningStatus() {
        // Given
        Experiment experiment = new Experiment(
            ExperimentId.generate(),
            "org-123",
            HypothesisId.generate(),
            "Test Experiment",
            "Description",
            "user-456"
        );
        experiment.start();

        // When
        experiment.cancel();

        // Then
        assertEquals(ExperimentStatus.CANCELLED, experiment.getStatus());
    }

    @Test
    void shouldUpdateExperimentInDraftStatus() {
        // Given
        Experiment experiment = new Experiment(
            ExperimentId.generate(),
            "org-123",
            HypothesisId.generate(),
            "Original Name",
            "Original Description",
            "user-456"
        );
        
        String newName = "Updated Name";
        String newDescription = "Updated Description";

        // When
        experiment.update(newName, newDescription);

        // Then
        assertEquals(newName, experiment.getName());
        assertEquals(newDescription, experiment.getDescription());
    }

    @Test
    void shouldThrowExceptionWhenStartingNonDraftExperiment() {
        // Given
        Experiment experiment = new Experiment(
            ExperimentId.generate(),
            "org-123",
            HypothesisId.generate(),
            "Test Experiment",
            "Description",
            "user-456"
        );
        experiment.start();

        // When & Then
        IllegalStateException exception = assertThrows(IllegalStateException.class, experiment::start);
        assertTrue(exception.getMessage().contains("DRAFT"));
    }

    @Test
    void shouldThrowExceptionWhenCompletingNonRunningExperiment() {
        // Given
        Experiment experiment = new Experiment(
            ExperimentId.generate(),
            "org-123",
            HypothesisId.generate(),
            "Test Experiment",
            "Description",
            "user-456"
        );

        // When & Then
        IllegalStateException exception = assertThrows(IllegalStateException.class, experiment::complete);
        assertTrue(exception.getMessage().contains("RUNNING"));
    }

    @Test
    void shouldThrowExceptionWhenCancellingCompletedExperiment() {
        // Given
        Experiment experiment = new Experiment(
            ExperimentId.generate(),
            "org-123",
            HypothesisId.generate(),
            "Test Experiment",
            "Description",
            "user-456"
        );
        experiment.start();
        experiment.complete();

        // When & Then
        assertThrows(IllegalStateException.class, experiment::cancel);
    }

    @Test
    void shouldThrowExceptionWhenUpdatingNonDraftExperiment() {
        // Given
        Experiment experiment = new Experiment(
            ExperimentId.generate(),
            "org-123",
            HypothesisId.generate(),
            "Test Experiment",
            "Description",
            "user-456"
        );
        experiment.start();

        // When & Then
        assertThrows(IllegalStateException.class, () -> {
            experiment.update("New Name", "New Description");
        });
    }

    @Test
    void shouldThrowExceptionWhenNameIsNull() {
        // Given
        ExperimentId id = ExperimentId.generate();

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            new Experiment(id, "org-123", HypothesisId.generate(), null, "Description", "user-456");
        });
    }

    @Test
    void shouldThrowExceptionWhenNameIsEmpty() {
        // Given
        ExperimentId id = ExperimentId.generate();

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            new Experiment(id, "org-123", HypothesisId.generate(), "", "Description", "user-456");
        });
    }

    @Test
    void shouldThrowExceptionWhenNameIsTooLong() {
        // Given
        ExperimentId id = ExperimentId.generate();
        String longName = "a".repeat(201);

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            new Experiment(id, "org-123", HypothesisId.generate(), longName, "Description", "user-456");
        });
    }

    @Test
    void shouldImplementTenantAware() {
        // Given
        Experiment experiment = new Experiment(
            ExperimentId.generate(),
            "org-123",
            HypothesisId.generate(),
            "Name",
            "Description",
            "user-456"
        );

        // When
        String newOrgId = "org-999";
        experiment.setOrganizationId(newOrgId);

        // Then
        assertEquals(newOrgId, experiment.getOrganizationId());
    }
}
