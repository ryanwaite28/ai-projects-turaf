package com.turaf.experiment.application;

import com.turaf.common.event.EventPublisher;
import com.turaf.common.tenant.TenantContextHolder;
import com.turaf.experiment.application.dto.CreateExperimentRequest;
import com.turaf.experiment.application.dto.ExperimentDto;
import com.turaf.experiment.application.dto.UpdateExperimentRequest;
import com.turaf.experiment.application.exception.ExperimentNotFoundException;
import com.turaf.experiment.application.exception.HypothesisNotFoundException;
import com.turaf.experiment.domain.*;
import com.turaf.experiment.domain.event.ExperimentCancelled;
import com.turaf.experiment.domain.event.ExperimentCompleted;
import com.turaf.experiment.domain.event.ExperimentCreated;
import com.turaf.experiment.domain.event.ExperimentStarted;
import com.turaf.experiment.domain.exception.InvalidStateTransitionException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExperimentServiceTest {

    @Mock
    private ExperimentRepository experimentRepository;

    @Mock
    private HypothesisRepository hypothesisRepository;

    @Mock
    private EventPublisher eventPublisher;

    @InjectMocks
    private ExperimentService experimentService;

    private static final String TEST_ORG_ID = "org-123";
    private static final String TEST_USER_ID = "user-456";
    private HypothesisId testHypothesisId;

    @BeforeEach
    void setUp() {
        TenantContextHolder.setOrganizationId(TEST_ORG_ID);
        testHypothesisId = HypothesisId.generate();
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    void shouldCreateExperiment() {
        // Given
        CreateExperimentRequest request = new CreateExperimentRequest(
            testHypothesisId.getValue(),
            "Cache Implementation Test",
            "Testing Redis caching performance"
        );

        Hypothesis hypothesis = new Hypothesis(
            testHypothesisId,
            TEST_ORG_ID,
            ProblemId.generate(),
            "Test Hypothesis",
            "Test Outcome",
            TEST_USER_ID
        );

        Experiment savedExperiment = new Experiment(
            ExperimentId.generate(),
            TEST_ORG_ID,
            testHypothesisId,
            request.getName(),
            request.getDescription(),
            TEST_USER_ID
        );

        when(hypothesisRepository.findById(testHypothesisId)).thenReturn(Optional.of(hypothesis));
        when(experimentRepository.save(any(Experiment.class))).thenReturn(savedExperiment);

        // When
        ExperimentDto result = experimentService.createExperiment(request, TEST_USER_ID);

        // Then
        assertNotNull(result);
        assertEquals(request.getName(), result.getName());
        assertEquals(request.getDescription(), result.getDescription());
        assertEquals(testHypothesisId.getValue(), result.getHypothesisId());
        assertEquals(TEST_ORG_ID, result.getOrganizationId());
        assertEquals(TEST_USER_ID, result.getCreatedBy());
        assertEquals("DRAFT", result.getStatus());

        verify(hypothesisRepository).findById(testHypothesisId);
        verify(experimentRepository).save(any(Experiment.class));
    }

    @Test
    void shouldThrowExceptionWhenCreatingExperimentWithInvalidHypothesisId() {
        // Given
        CreateExperimentRequest request = new CreateExperimentRequest(
            testHypothesisId.getValue(),
            "Test Experiment",
            "Test Description"
        );

        when(hypothesisRepository.findById(testHypothesisId)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(HypothesisNotFoundException.class, () -> {
            experimentService.createExperiment(request, TEST_USER_ID);
        });

        verify(hypothesisRepository).findById(testHypothesisId);
        verify(experimentRepository, never()).save(any(Experiment.class));
    }

    @Test
    void shouldPublishDomainEventsOnCreate() {
        // Given
        CreateExperimentRequest request = new CreateExperimentRequest(
            testHypothesisId.getValue(),
            "Test Experiment",
            "Test Description"
        );

        Hypothesis hypothesis = new Hypothesis(
            testHypothesisId,
            TEST_ORG_ID,
            ProblemId.generate(),
            "Test Hypothesis",
            "Test Outcome",
            TEST_USER_ID
        );

        Experiment savedExperiment = new Experiment(
            ExperimentId.generate(),
            TEST_ORG_ID,
            testHypothesisId,
            request.getName(),
            request.getDescription(),
            TEST_USER_ID
        );

        when(hypothesisRepository.findById(testHypothesisId)).thenReturn(Optional.of(hypothesis));
        when(experimentRepository.save(any(Experiment.class))).thenReturn(savedExperiment);

        // When
        experimentService.createExperiment(request, TEST_USER_ID);

        // Then
        ArgumentCaptor<ExperimentCreated> eventCaptor = ArgumentCaptor.forClass(ExperimentCreated.class);
        verify(eventPublisher).publish(eventCaptor.capture());

        ExperimentCreated event = eventCaptor.getValue();
        assertEquals(savedExperiment.getId().getValue(), event.getExperimentId());
        assertEquals(TEST_ORG_ID, event.getOrganizationId());
        assertEquals(testHypothesisId.getValue(), event.getHypothesisId());
    }

    @Test
    void shouldGetExperimentById() {
        // Given
        ExperimentId experimentId = ExperimentId.generate();
        Experiment experiment = new Experiment(
            experimentId,
            TEST_ORG_ID,
            testHypothesisId,
            "Test Experiment",
            "Test Description",
            TEST_USER_ID
        );

        when(experimentRepository.findById(experimentId)).thenReturn(Optional.of(experiment));

        // When
        ExperimentDto result = experimentService.getExperiment(experimentId);

        // Then
        assertNotNull(result);
        assertEquals(experimentId.getValue(), result.getId());
        assertEquals(experiment.getName(), result.getName());
        assertEquals(experiment.getDescription(), result.getDescription());
        assertEquals("DRAFT", result.getStatus());

        verify(experimentRepository).findById(experimentId);
    }

    @Test
    void shouldThrowExceptionWhenExperimentNotFound() {
        // Given
        ExperimentId experimentId = ExperimentId.generate();
        when(experimentRepository.findById(experimentId)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(ExperimentNotFoundException.class, () -> {
            experimentService.getExperiment(experimentId);
        });

        verify(experimentRepository).findById(experimentId);
    }

    @Test
    void shouldGetExperimentsByHypothesis() {
        // Given
        Experiment experiment1 = new Experiment(
            ExperimentId.generate(),
            TEST_ORG_ID,
            testHypothesisId,
            "Experiment 1",
            "Description 1",
            TEST_USER_ID
        );
        Experiment experiment2 = new Experiment(
            ExperimentId.generate(),
            TEST_ORG_ID,
            testHypothesisId,
            "Experiment 2",
            "Description 2",
            TEST_USER_ID
        );

        when(experimentRepository.findByHypothesisId(testHypothesisId))
            .thenReturn(List.of(experiment1, experiment2));

        // When
        List<ExperimentDto> results = experimentService.getExperimentsByHypothesis(testHypothesisId);

        // Then
        assertEquals(2, results.size());
        assertEquals("Experiment 1", results.get(0).getName());
        assertEquals("Experiment 2", results.get(1).getName());
        assertTrue(results.stream().allMatch(e -> e.getHypothesisId().equals(testHypothesisId.getValue())));

        verify(experimentRepository).findByHypothesisId(testHypothesisId);
    }

    @Test
    void shouldGetExperimentsByStatus() {
        // Given
        Experiment draftExperiment = new Experiment(
            ExperimentId.generate(),
            TEST_ORG_ID,
            testHypothesisId,
            "Draft Experiment",
            "Description",
            TEST_USER_ID
        );

        when(experimentRepository.findByOrganizationIdAndStatus(TEST_ORG_ID, ExperimentStatus.DRAFT))
            .thenReturn(List.of(draftExperiment));

        // When
        List<ExperimentDto> results = experimentService.getExperimentsByStatus(ExperimentStatus.DRAFT);

        // Then
        assertEquals(1, results.size());
        assertEquals("DRAFT", results.get(0).getStatus());

        verify(experimentRepository).findByOrganizationIdAndStatus(TEST_ORG_ID, ExperimentStatus.DRAFT);
    }

    @Test
    void shouldGetAllExperimentsForOrganization() {
        // Given
        Experiment experiment1 = new Experiment(
            ExperimentId.generate(),
            TEST_ORG_ID,
            testHypothesisId,
            "Experiment 1",
            "Description 1",
            TEST_USER_ID
        );
        Experiment experiment2 = new Experiment(
            ExperimentId.generate(),
            TEST_ORG_ID,
            testHypothesisId,
            "Experiment 2",
            "Description 2",
            TEST_USER_ID
        );

        when(experimentRepository.findByOrganizationId(TEST_ORG_ID))
            .thenReturn(List.of(experiment1, experiment2));

        // When
        List<ExperimentDto> results = experimentService.getAllExperiments();

        // Then
        assertEquals(2, results.size());
        assertTrue(results.stream().allMatch(e -> e.getOrganizationId().equals(TEST_ORG_ID)));

        verify(experimentRepository).findByOrganizationId(TEST_ORG_ID);
    }

    @Test
    void shouldUpdateExperiment() {
        // Given
        ExperimentId experimentId = ExperimentId.generate();
        Experiment existingExperiment = new Experiment(
            experimentId,
            TEST_ORG_ID,
            testHypothesisId,
            "Original Name",
            "Original Description",
            TEST_USER_ID
        );

        UpdateExperimentRequest request = new UpdateExperimentRequest(
            "Updated Name",
            "Updated Description"
        );

        when(experimentRepository.findById(experimentId)).thenReturn(Optional.of(existingExperiment));
        when(experimentRepository.save(any(Experiment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        ExperimentDto result = experimentService.updateExperiment(experimentId, request);

        // Then
        assertNotNull(result);
        assertEquals("Updated Name", result.getName());
        assertEquals("Updated Description", result.getDescription());

        verify(experimentRepository).findById(experimentId);
        verify(experimentRepository).save(any(Experiment.class));
    }

    @Test
    void shouldStartExperiment() {
        // Given
        ExperimentId experimentId = ExperimentId.generate();
        Experiment experiment = new Experiment(
            experimentId,
            TEST_ORG_ID,
            testHypothesisId,
            "Test Experiment",
            "Test Description",
            TEST_USER_ID
        );

        when(experimentRepository.findById(experimentId)).thenReturn(Optional.of(experiment));
        when(experimentRepository.save(any(Experiment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        ExperimentDto result = experimentService.startExperiment(experimentId);

        // Then
        assertNotNull(result);
        assertEquals("RUNNING", result.getStatus());
        assertNotNull(result.getStartedAt());

        verify(experimentRepository).findById(experimentId);
        verify(experimentRepository).save(any(Experiment.class));
    }

    @Test
    void shouldPublishEventWhenStartingExperiment() {
        // Given
        ExperimentId experimentId = ExperimentId.generate();
        Experiment experiment = new Experiment(
            experimentId,
            TEST_ORG_ID,
            testHypothesisId,
            "Test Experiment",
            "Test Description",
            TEST_USER_ID
        );

        when(experimentRepository.findById(experimentId)).thenReturn(Optional.of(experiment));
        when(experimentRepository.save(any(Experiment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        experimentService.startExperiment(experimentId);

        // Then
        ArgumentCaptor<ExperimentStarted> eventCaptor = ArgumentCaptor.forClass(ExperimentStarted.class);
        verify(eventPublisher).publish(eventCaptor.capture());

        ExperimentStarted event = eventCaptor.getValue();
        assertEquals(experimentId.getValue(), event.getExperimentId());
        assertEquals(TEST_ORG_ID, event.getOrganizationId());
    }

    @Test
    void shouldCompleteExperiment() {
        // Given
        ExperimentId experimentId = ExperimentId.generate();
        Experiment experiment = new Experiment(
            experimentId,
            TEST_ORG_ID,
            testHypothesisId,
            "Test Experiment",
            "Test Description",
            TEST_USER_ID
        );
        experiment.start();

        when(experimentRepository.findById(experimentId)).thenReturn(Optional.of(experiment));
        when(experimentRepository.save(any(Experiment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        ExperimentDto result = experimentService.completeExperiment(experimentId);

        // Then
        assertNotNull(result);
        assertEquals("COMPLETED", result.getStatus());
        assertNotNull(result.getCompletedAt());

        verify(experimentRepository).findById(experimentId);
        verify(experimentRepository).save(any(Experiment.class));
    }

    @Test
    void shouldPublishEventWhenCompletingExperiment() {
        // Given
        ExperimentId experimentId = ExperimentId.generate();
        Experiment experiment = new Experiment(
            experimentId,
            TEST_ORG_ID,
            testHypothesisId,
            "Test Experiment",
            "Test Description",
            TEST_USER_ID
        );
        experiment.start();

        when(experimentRepository.findById(experimentId)).thenReturn(Optional.of(experiment));
        when(experimentRepository.save(any(Experiment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        experimentService.completeExperiment(experimentId);

        // Then
        ArgumentCaptor<ExperimentCompleted> eventCaptor = ArgumentCaptor.forClass(ExperimentCompleted.class);
        verify(eventPublisher).publish(eventCaptor.capture());

        ExperimentCompleted event = eventCaptor.getValue();
        assertEquals(experimentId.getValue(), event.getExperimentId());
        assertEquals(TEST_ORG_ID, event.getOrganizationId());
    }

    @Test
    void shouldCancelExperiment() {
        // Given
        ExperimentId experimentId = ExperimentId.generate();
        Experiment experiment = new Experiment(
            experimentId,
            TEST_ORG_ID,
            testHypothesisId,
            "Test Experiment",
            "Test Description",
            TEST_USER_ID
        );

        when(experimentRepository.findById(experimentId)).thenReturn(Optional.of(experiment));
        when(experimentRepository.save(any(Experiment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        ExperimentDto result = experimentService.cancelExperiment(experimentId);

        // Then
        assertNotNull(result);
        assertEquals("CANCELLED", result.getStatus());

        verify(experimentRepository).findById(experimentId);
        verify(experimentRepository).save(any(Experiment.class));
    }

    @Test
    void shouldPublishEventWhenCancellingExperiment() {
        // Given
        ExperimentId experimentId = ExperimentId.generate();
        Experiment experiment = new Experiment(
            experimentId,
            TEST_ORG_ID,
            testHypothesisId,
            "Test Experiment",
            "Test Description",
            TEST_USER_ID
        );

        when(experimentRepository.findById(experimentId)).thenReturn(Optional.of(experiment));
        when(experimentRepository.save(any(Experiment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        experimentService.cancelExperiment(experimentId);

        // Then
        ArgumentCaptor<ExperimentCancelled> eventCaptor = ArgumentCaptor.forClass(ExperimentCancelled.class);
        verify(eventPublisher).publish(eventCaptor.capture());

        ExperimentCancelled event = eventCaptor.getValue();
        assertEquals(experimentId.getValue(), event.getExperimentId());
        assertEquals(TEST_ORG_ID, event.getOrganizationId());
    }

    @Test
    void shouldThrowExceptionForInvalidStateTransition() {
        // Given
        ExperimentId experimentId = ExperimentId.generate();
        Experiment experiment = new Experiment(
            experimentId,
            TEST_ORG_ID,
            testHypothesisId,
            "Test Experiment",
            "Test Description",
            TEST_USER_ID
        );

        when(experimentRepository.findById(experimentId)).thenReturn(Optional.of(experiment));

        // When & Then - Try to complete without starting
        assertThrows(InvalidStateTransitionException.class, () -> {
            experimentService.completeExperiment(experimentId);
        });

        verify(experimentRepository).findById(experimentId);
        verify(experimentRepository, never()).save(any(Experiment.class));
    }

    @Test
    void shouldDeleteExperiment() {
        // Given
        ExperimentId experimentId = ExperimentId.generate();
        Experiment experiment = new Experiment(
            experimentId,
            TEST_ORG_ID,
            testHypothesisId,
            "Test Experiment",
            "Test Description",
            TEST_USER_ID
        );

        when(experimentRepository.findById(experimentId)).thenReturn(Optional.of(experiment));
        doNothing().when(experimentRepository).delete(experiment);

        // When
        experimentService.deleteExperiment(experimentId);

        // Then
        verify(experimentRepository).findById(experimentId);
        verify(experimentRepository).delete(experiment);
    }

    @Test
    void shouldIncludeAllowedTransitionsInDto() {
        // Given
        ExperimentId experimentId = ExperimentId.generate();
        Experiment experiment = new Experiment(
            experimentId,
            TEST_ORG_ID,
            testHypothesisId,
            "Test Experiment",
            "Test Description",
            TEST_USER_ID
        );

        when(experimentRepository.findById(experimentId)).thenReturn(Optional.of(experiment));

        // When
        ExperimentDto result = experimentService.getExperiment(experimentId);

        // Then
        assertNotNull(result.getAllowedTransitions());
        assertEquals(2, result.getAllowedTransitions().size());
        assertTrue(result.getAllowedTransitions().contains("RUNNING"));
        assertTrue(result.getAllowedTransitions().contains("CANCELLED"));
    }

    @Test
    void shouldClearDomainEventsAfterPublishing() {
        // Given
        ExperimentId experimentId = ExperimentId.generate();
        Experiment experiment = new Experiment(
            experimentId,
            TEST_ORG_ID,
            testHypothesisId,
            "Test Experiment",
            "Test Description",
            TEST_USER_ID
        );

        when(experimentRepository.findById(experimentId)).thenReturn(Optional.of(experiment));
        when(experimentRepository.save(any(Experiment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        experimentService.startExperiment(experimentId);

        // Then
        verify(eventPublisher).publish(any(ExperimentStarted.class));
        assertEquals(0, experiment.getDomainEvents().size());
    }
}
