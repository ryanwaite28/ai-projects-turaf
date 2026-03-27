package com.turaf.experiment.application;

import com.turaf.common.event.EventPublisher;
import com.turaf.common.tenant.TenantContextHolder;
import com.turaf.experiment.application.dto.CreateHypothesisRequest;
import com.turaf.experiment.application.dto.HypothesisDto;
import com.turaf.experiment.application.dto.UpdateHypothesisRequest;
import com.turaf.experiment.application.exception.HypothesisNotFoundException;
import com.turaf.experiment.application.exception.ProblemNotFoundException;
import com.turaf.experiment.domain.*;
import com.turaf.experiment.domain.event.HypothesisCreated;
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
class HypothesisServiceTest {

    @Mock
    private HypothesisRepository hypothesisRepository;

    @Mock
    private ProblemRepository problemRepository;

    @Mock
    private EventPublisher eventPublisher;

    @InjectMocks
    private HypothesisService hypothesisService;

    private static final String TEST_ORG_ID = "org-123";
    private static final String TEST_USER_ID = "user-456";
    private ProblemId testProblemId;

    @BeforeEach
    void setUp() {
        TenantContextHolder.setOrganizationId(TEST_ORG_ID);
        testProblemId = ProblemId.generate();
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    void shouldCreateHypothesis() {
        // Given
        CreateHypothesisRequest request = new CreateHypothesisRequest(
            testProblemId.getValue(),
            "Implementing caching will reduce login time by 50%",
            "Login time < 500ms"
        );

        Problem problem = new Problem(
            testProblemId,
            TEST_ORG_ID,
            "Test Problem",
            "Description",
            TEST_USER_ID
        );

        Hypothesis savedHypothesis = new Hypothesis(
            HypothesisId.generate(),
            TEST_ORG_ID,
            testProblemId,
            request.getStatement(),
            request.getExpectedOutcome(),
            TEST_USER_ID
        );

        when(problemRepository.findById(testProblemId)).thenReturn(Optional.of(problem));
        when(hypothesisRepository.save(any(Hypothesis.class))).thenReturn(savedHypothesis);

        // When
        HypothesisDto result = hypothesisService.createHypothesis(request, TEST_USER_ID);

        // Then
        assertNotNull(result);
        assertEquals(request.getStatement(), result.getStatement());
        assertEquals(request.getExpectedOutcome(), result.getExpectedOutcome());
        assertEquals(testProblemId.getValue(), result.getProblemId());
        assertEquals(TEST_ORG_ID, result.getOrganizationId());
        assertEquals(TEST_USER_ID, result.getCreatedBy());

        verify(problemRepository).findById(testProblemId);
        verify(hypothesisRepository).save(any(Hypothesis.class));
    }

    @Test
    void shouldThrowExceptionWhenCreatingHypothesisWithInvalidProblemId() {
        // Given
        CreateHypothesisRequest request = new CreateHypothesisRequest(
            testProblemId.getValue(),
            "Test Statement",
            "Test Outcome"
        );

        when(problemRepository.findById(testProblemId)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(ProblemNotFoundException.class, () -> {
            hypothesisService.createHypothesis(request, TEST_USER_ID);
        });

        verify(problemRepository).findById(testProblemId);
        verify(hypothesisRepository, never()).save(any(Hypothesis.class));
    }

    @Test
    void shouldPublishDomainEventsOnCreate() {
        // Given
        CreateHypothesisRequest request = new CreateHypothesisRequest(
            testProblemId.getValue(),
            "Test Statement",
            "Test Outcome"
        );

        Problem problem = new Problem(
            testProblemId,
            TEST_ORG_ID,
            "Test Problem",
            "Description",
            TEST_USER_ID
        );

        Hypothesis savedHypothesis = new Hypothesis(
            HypothesisId.generate(),
            TEST_ORG_ID,
            testProblemId,
            request.getStatement(),
            request.getExpectedOutcome(),
            TEST_USER_ID
        );

        when(problemRepository.findById(testProblemId)).thenReturn(Optional.of(problem));
        when(hypothesisRepository.save(any(Hypothesis.class))).thenReturn(savedHypothesis);

        // When
        hypothesisService.createHypothesis(request, TEST_USER_ID);

        // Then
        ArgumentCaptor<HypothesisCreated> eventCaptor = ArgumentCaptor.forClass(HypothesisCreated.class);
        verify(eventPublisher).publish(eventCaptor.capture());

        HypothesisCreated event = eventCaptor.getValue();
        assertEquals(savedHypothesis.getId().getValue(), event.getHypothesisId());
        assertEquals(TEST_ORG_ID, event.getOrganizationId());
        assertEquals(testProblemId.getValue(), event.getProblemId());
    }

    @Test
    void shouldGetHypothesisById() {
        // Given
        HypothesisId hypothesisId = HypothesisId.generate();
        Hypothesis hypothesis = new Hypothesis(
            hypothesisId,
            TEST_ORG_ID,
            testProblemId,
            "Test Statement",
            "Test Outcome",
            TEST_USER_ID
        );

        when(hypothesisRepository.findById(hypothesisId)).thenReturn(Optional.of(hypothesis));

        // When
        HypothesisDto result = hypothesisService.getHypothesis(hypothesisId);

        // Then
        assertNotNull(result);
        assertEquals(hypothesisId.getValue(), result.getId());
        assertEquals(hypothesis.getStatement(), result.getStatement());
        assertEquals(hypothesis.getExpectedOutcome(), result.getExpectedOutcome());

        verify(hypothesisRepository).findById(hypothesisId);
    }

    @Test
    void shouldThrowExceptionWhenHypothesisNotFound() {
        // Given
        HypothesisId hypothesisId = HypothesisId.generate();
        when(hypothesisRepository.findById(hypothesisId)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(HypothesisNotFoundException.class, () -> {
            hypothesisService.getHypothesis(hypothesisId);
        });

        verify(hypothesisRepository).findById(hypothesisId);
    }

    @Test
    void shouldGetHypothesesByProblem() {
        // Given
        Hypothesis hypothesis1 = new Hypothesis(
            HypothesisId.generate(),
            TEST_ORG_ID,
            testProblemId,
            "Hypothesis 1",
            "Outcome 1",
            TEST_USER_ID
        );
        Hypothesis hypothesis2 = new Hypothesis(
            HypothesisId.generate(),
            TEST_ORG_ID,
            testProblemId,
            "Hypothesis 2",
            "Outcome 2",
            TEST_USER_ID
        );

        when(hypothesisRepository.findByProblemId(testProblemId))
            .thenReturn(List.of(hypothesis1, hypothesis2));

        // When
        List<HypothesisDto> results = hypothesisService.getHypothesesByProblem(testProblemId);

        // Then
        assertEquals(2, results.size());
        assertEquals("Hypothesis 1", results.get(0).getStatement());
        assertEquals("Hypothesis 2", results.get(1).getStatement());
        assertTrue(results.stream().allMatch(h -> h.getProblemId().equals(testProblemId.getValue())));

        verify(hypothesisRepository).findByProblemId(testProblemId);
    }

    @Test
    void shouldReturnEmptyListWhenNoHypothesesExistForProblem() {
        // Given
        when(hypothesisRepository.findByProblemId(testProblemId))
            .thenReturn(List.of());

        // When
        List<HypothesisDto> results = hypothesisService.getHypothesesByProblem(testProblemId);

        // Then
        assertTrue(results.isEmpty());
        verify(hypothesisRepository).findByProblemId(testProblemId);
    }

    @Test
    void shouldGetAllHypothesesForOrganization() {
        // Given
        Hypothesis hypothesis1 = new Hypothesis(
            HypothesisId.generate(),
            TEST_ORG_ID,
            testProblemId,
            "Hypothesis 1",
            "Outcome 1",
            TEST_USER_ID
        );
        Hypothesis hypothesis2 = new Hypothesis(
            HypothesisId.generate(),
            TEST_ORG_ID,
            testProblemId,
            "Hypothesis 2",
            "Outcome 2",
            TEST_USER_ID
        );

        when(hypothesisRepository.findByOrganizationId(TEST_ORG_ID))
            .thenReturn(List.of(hypothesis1, hypothesis2));

        // When
        List<HypothesisDto> results = hypothesisService.getAllHypotheses();

        // Then
        assertEquals(2, results.size());
        assertTrue(results.stream().allMatch(h -> h.getOrganizationId().equals(TEST_ORG_ID)));

        verify(hypothesisRepository).findByOrganizationId(TEST_ORG_ID);
    }

    @Test
    void shouldUpdateHypothesis() {
        // Given
        HypothesisId hypothesisId = HypothesisId.generate();
        Hypothesis existingHypothesis = new Hypothesis(
            hypothesisId,
            TEST_ORG_ID,
            testProblemId,
            "Original Statement",
            "Original Outcome",
            TEST_USER_ID
        );

        UpdateHypothesisRequest request = new UpdateHypothesisRequest(
            "Updated Statement",
            "Updated Outcome"
        );

        when(hypothesisRepository.findById(hypothesisId)).thenReturn(Optional.of(existingHypothesis));
        when(hypothesisRepository.save(any(Hypothesis.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        HypothesisDto result = hypothesisService.updateHypothesis(hypothesisId, request);

        // Then
        assertNotNull(result);
        assertEquals("Updated Statement", result.getStatement());
        assertEquals("Updated Outcome", result.getExpectedOutcome());

        verify(hypothesisRepository).findById(hypothesisId);
        verify(hypothesisRepository).save(any(Hypothesis.class));
    }

    @Test
    void shouldThrowExceptionWhenUpdatingNonExistentHypothesis() {
        // Given
        HypothesisId hypothesisId = HypothesisId.generate();
        UpdateHypothesisRequest request = new UpdateHypothesisRequest(
            "Updated Statement",
            "Updated Outcome"
        );

        when(hypothesisRepository.findById(hypothesisId)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(HypothesisNotFoundException.class, () -> {
            hypothesisService.updateHypothesis(hypothesisId, request);
        });

        verify(hypothesisRepository).findById(hypothesisId);
        verify(hypothesisRepository, never()).save(any(Hypothesis.class));
    }

    @Test
    void shouldDeleteHypothesis() {
        // Given
        HypothesisId hypothesisId = HypothesisId.generate();
        Hypothesis hypothesis = new Hypothesis(
            hypothesisId,
            TEST_ORG_ID,
            testProblemId,
            "Test Statement",
            "Test Outcome",
            TEST_USER_ID
        );

        when(hypothesisRepository.findById(hypothesisId)).thenReturn(Optional.of(hypothesis));
        doNothing().when(hypothesisRepository).delete(hypothesis);

        // When
        hypothesisService.deleteHypothesis(hypothesisId);

        // Then
        verify(hypothesisRepository).findById(hypothesisId);
        verify(hypothesisRepository).delete(hypothesis);
    }

    @Test
    void shouldThrowExceptionWhenDeletingNonExistentHypothesis() {
        // Given
        HypothesisId hypothesisId = HypothesisId.generate();
        when(hypothesisRepository.findById(hypothesisId)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(HypothesisNotFoundException.class, () -> {
            hypothesisService.deleteHypothesis(hypothesisId);
        });

        verify(hypothesisRepository).findById(hypothesisId);
        verify(hypothesisRepository, never()).delete(any(Hypothesis.class));
    }

    @Test
    void shouldUseTenantContextForOrganizationId() {
        // Given
        CreateHypothesisRequest request = new CreateHypothesisRequest(
            testProblemId.getValue(),
            "Test Statement",
            "Test Outcome"
        );

        Problem problem = new Problem(
            testProblemId,
            TEST_ORG_ID,
            "Test Problem",
            "Description",
            TEST_USER_ID
        );

        Hypothesis savedHypothesis = new Hypothesis(
            HypothesisId.generate(),
            TEST_ORG_ID,
            testProblemId,
            request.getStatement(),
            request.getExpectedOutcome(),
            TEST_USER_ID
        );

        when(problemRepository.findById(testProblemId)).thenReturn(Optional.of(problem));
        when(hypothesisRepository.save(any(Hypothesis.class))).thenReturn(savedHypothesis);

        // When
        HypothesisDto result = hypothesisService.createHypothesis(request, TEST_USER_ID);

        // Then
        assertEquals(TEST_ORG_ID, result.getOrganizationId());

        ArgumentCaptor<Hypothesis> hypothesisCaptor = ArgumentCaptor.forClass(Hypothesis.class);
        verify(hypothesisRepository).save(hypothesisCaptor.capture());
        assertEquals(TEST_ORG_ID, hypothesisCaptor.getValue().getOrganizationId());
    }

    @Test
    void shouldClearDomainEventsAfterPublishing() {
        // Given
        CreateHypothesisRequest request = new CreateHypothesisRequest(
            testProblemId.getValue(),
            "Test Statement",
            "Test Outcome"
        );

        Problem problem = new Problem(
            testProblemId,
            TEST_ORG_ID,
            "Test Problem",
            "Description",
            TEST_USER_ID
        );

        Hypothesis savedHypothesis = new Hypothesis(
            HypothesisId.generate(),
            TEST_ORG_ID,
            testProblemId,
            request.getStatement(),
            request.getExpectedOutcome(),
            TEST_USER_ID
        );

        // Verify hypothesis has events before save
        assertEquals(1, savedHypothesis.getDomainEvents().size());

        when(problemRepository.findById(testProblemId)).thenReturn(Optional.of(problem));
        when(hypothesisRepository.save(any(Hypothesis.class))).thenReturn(savedHypothesis);

        // When
        hypothesisService.createHypothesis(request, TEST_USER_ID);

        // Then
        verify(eventPublisher).publish(any(HypothesisCreated.class));
        // Events should be cleared after publishing
        assertEquals(0, savedHypothesis.getDomainEvents().size());
    }
}
