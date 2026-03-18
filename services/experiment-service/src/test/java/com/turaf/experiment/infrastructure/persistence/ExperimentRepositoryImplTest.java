package com.turaf.experiment.infrastructure.persistence;

import com.turaf.experiment.domain.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
@Import({ExperimentRepositoryImpl.class, HypothesisRepositoryImpl.class, ProblemRepositoryImpl.class})
class ExperimentRepositoryImplTest {

    @Autowired
    private ExperimentRepositoryImpl experimentRepository;

    @Autowired
    private HypothesisRepositoryImpl hypothesisRepository;

    @Autowired
    private ProblemRepositoryImpl problemRepository;

    @Autowired
    private ExperimentJpaRepository jpaRepository;

    private HypothesisId hypothesisId;

    @BeforeEach
    void setUp() {
        jpaRepository.deleteAll();
        
        // Create problem and hypothesis for foreign key constraints
        Problem problem = new Problem(
            ProblemId.generate(),
            "org-123",
            "Test Problem",
            "Test Description",
            "user-456"
        );
        Problem savedProblem = problemRepository.save(problem);

        Hypothesis hypothesis = new Hypothesis(
            HypothesisId.generate(),
            "org-123",
            savedProblem.getId(),
            "Test Hypothesis",
            "Test Outcome",
            "user-456"
        );
        Hypothesis savedHypothesis = hypothesisRepository.save(hypothesis);
        hypothesisId = savedHypothesis.getId();
    }

    @Test
    void shouldSaveAndRetrieveExperiment() {
        // Given
        Experiment experiment = new Experiment(
            ExperimentId.generate(),
            "org-123",
            hypothesisId,
            "Cache Implementation Test",
            "Testing Redis caching",
            "user-456"
        );

        // When
        Experiment saved = experimentRepository.save(experiment);
        Optional<Experiment> retrieved = experimentRepository.findById(saved.getId());

        // Then
        assertTrue(retrieved.isPresent());
        assertEquals(saved.getId(), retrieved.get().getId());
        assertEquals(saved.getName(), retrieved.get().getName());
        assertEquals(ExperimentStatus.DRAFT, retrieved.get().getStatus());
    }

    @Test
    void shouldFindExperimentsByHypothesisId() {
        // Given
        Experiment experiment1 = new Experiment(
            ExperimentId.generate(),
            "org-123",
            hypothesisId,
            "Experiment 1",
            "Description 1",
            "user-1"
        );
        Experiment experiment2 = new Experiment(
            ExperimentId.generate(),
            "org-123",
            hypothesisId,
            "Experiment 2",
            "Description 2",
            "user-1"
        );

        experimentRepository.save(experiment1);
        experimentRepository.save(experiment2);

        // When
        List<Experiment> experiments = experimentRepository.findByHypothesisId(hypothesisId);

        // Then
        assertEquals(2, experiments.size());
        assertTrue(experiments.stream().allMatch(e -> e.getHypothesisId().equals(hypothesisId)));
    }

    @Test
    void shouldFindExperimentsByStatus() {
        // Given
        Experiment draftExperiment = new Experiment(
            ExperimentId.generate(),
            "org-123",
            hypothesisId,
            "Draft Experiment",
            "Description",
            "user-1"
        );
        Experiment runningExperiment = new Experiment(
            ExperimentId.generate(),
            "org-123",
            hypothesisId,
            "Running Experiment",
            "Description",
            "user-1"
        );
        
        experimentRepository.save(draftExperiment);
        Experiment saved = experimentRepository.save(runningExperiment);
        saved.start();
        experimentRepository.save(saved);

        // When
        List<Experiment> draftExperiments = experimentRepository.findByStatus(ExperimentStatus.DRAFT);
        List<Experiment> runningExperiments = experimentRepository.findByStatus(ExperimentStatus.RUNNING);

        // Then
        assertEquals(1, draftExperiments.size());
        assertEquals(1, runningExperiments.size());
        assertEquals(ExperimentStatus.DRAFT, draftExperiments.get(0).getStatus());
        assertEquals(ExperimentStatus.RUNNING, runningExperiments.get(0).getStatus());
    }

    @Test
    void shouldFindExperimentsByOrganizationId() {
        // Given
        String orgId = "org-123";
        Experiment experiment1 = new Experiment(
            ExperimentId.generate(),
            orgId,
            hypothesisId,
            "Experiment 1",
            "Description 1",
            "user-1"
        );
        Experiment experiment2 = new Experiment(
            ExperimentId.generate(),
            orgId,
            hypothesisId,
            "Experiment 2",
            "Description 2",
            "user-1"
        );

        experimentRepository.save(experiment1);
        experimentRepository.save(experiment2);

        // When
        List<Experiment> experiments = experimentRepository.findByOrganizationId(orgId);

        // Then
        assertEquals(2, experiments.size());
        assertTrue(experiments.stream().allMatch(e -> e.getOrganizationId().equals(orgId)));
    }

    @Test
    void shouldFindExperimentsByOrganizationIdAndStatus() {
        // Given
        String orgId = "org-123";
        Experiment draftExperiment = new Experiment(
            ExperimentId.generate(),
            orgId,
            hypothesisId,
            "Draft Experiment",
            "Description",
            "user-1"
        );
        Experiment runningExperiment = new Experiment(
            ExperimentId.generate(),
            orgId,
            hypothesisId,
            "Running Experiment",
            "Description",
            "user-1"
        );
        
        experimentRepository.save(draftExperiment);
        Experiment saved = experimentRepository.save(runningExperiment);
        saved.start();
        experimentRepository.save(saved);

        // When
        List<Experiment> draftExperiments = experimentRepository.findByOrganizationIdAndStatus(orgId, ExperimentStatus.DRAFT);

        // Then
        assertEquals(1, draftExperiments.size());
        assertEquals(ExperimentStatus.DRAFT, draftExperiments.get(0).getStatus());
        assertEquals(orgId, draftExperiments.get(0).getOrganizationId());
    }

    @Test
    void shouldPersistExperimentStateTransitions() {
        // Given
        Experiment experiment = new Experiment(
            ExperimentId.generate(),
            "org-123",
            hypothesisId,
            "State Test Experiment",
            "Testing state persistence",
            "user-456"
        );
        Experiment saved = experimentRepository.save(experiment);

        // When - Transition to RUNNING
        saved.start();
        experimentRepository.save(saved);
        Optional<Experiment> running = experimentRepository.findById(saved.getId());

        // Then
        assertTrue(running.isPresent());
        assertEquals(ExperimentStatus.RUNNING, running.get().getStatus());
        assertNotNull(running.get().getStartedAt());

        // When - Transition to COMPLETED
        running.get().complete();
        experimentRepository.save(running.get());
        Optional<Experiment> completed = experimentRepository.findById(saved.getId());

        // Then
        assertTrue(completed.isPresent());
        assertEquals(ExperimentStatus.COMPLETED, completed.get().getStatus());
        assertNotNull(completed.get().getCompletedAt());
    }

    @Test
    void shouldDeleteExperiment() {
        // Given
        Experiment experiment = new Experiment(
            ExperimentId.generate(),
            "org-123",
            hypothesisId,
            "Test Experiment",
            "Test Description",
            "user-456"
        );
        Experiment saved = experimentRepository.save(experiment);

        // When
        experimentRepository.delete(saved);
        Optional<Experiment> retrieved = experimentRepository.findById(saved.getId());

        // Then
        assertFalse(retrieved.isPresent());
    }

    @Test
    void shouldCascadeDeleteWhenHypothesisIsDeleted() {
        // Given
        Experiment experiment = new Experiment(
            ExperimentId.generate(),
            "org-123",
            hypothesisId,
            "Test Experiment",
            "Test Description",
            "user-456"
        );
        Experiment saved = experimentRepository.save(experiment);

        // When
        Optional<Hypothesis> hypothesis = hypothesisRepository.findById(hypothesisId);
        assertTrue(hypothesis.isPresent());
        hypothesisRepository.delete(hypothesis.get());

        // Then
        Optional<Experiment> retrieved = experimentRepository.findById(saved.getId());
        assertFalse(retrieved.isPresent());
    }
}
