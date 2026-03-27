package com.turaf.experiment.application;

import com.turaf.common.event.EventPublisher;
import com.turaf.common.tenant.TenantContextHolder;
import com.turaf.experiment.application.dto.CreateProblemRequest;
import com.turaf.experiment.application.dto.ProblemDto;
import com.turaf.experiment.application.dto.UpdateProblemRequest;
import com.turaf.experiment.application.exception.ProblemNotFoundException;
import com.turaf.experiment.domain.Problem;
import com.turaf.experiment.domain.ProblemId;
import com.turaf.experiment.domain.ProblemRepository;
import com.turaf.experiment.domain.event.ProblemCreated;
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
class ProblemServiceTest {

    @Mock
    private ProblemRepository problemRepository;

    @Mock
    private EventPublisher eventPublisher;

    @InjectMocks
    private ProblemService problemService;

    private static final String TEST_ORG_ID = "org-123";
    private static final String TEST_USER_ID = "user-456";

    @BeforeEach
    void setUp() {
        TenantContextHolder.setOrganizationId(TEST_ORG_ID);
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    void shouldCreateProblem() {
        // Given
        CreateProblemRequest request = new CreateProblemRequest(
            "User authentication is slow",
            "Users experiencing delays during login"
        );

        Problem savedProblem = new Problem(
            ProblemId.generate(),
            TEST_ORG_ID,
            request.getTitle(),
            request.getDescription(),
            TEST_USER_ID
        );

        when(problemRepository.save(any(Problem.class))).thenReturn(savedProblem);

        // When
        ProblemDto result = problemService.createProblem(request, TEST_USER_ID);

        // Then
        assertNotNull(result);
        assertEquals(request.getTitle(), result.getTitle());
        assertEquals(request.getDescription(), result.getDescription());
        assertEquals(TEST_ORG_ID, result.getOrganizationId());
        assertEquals(TEST_USER_ID, result.getCreatedBy());

        verify(problemRepository).save(any(Problem.class));
    }

    @Test
    void shouldPublishDomainEventsOnCreate() {
        // Given
        CreateProblemRequest request = new CreateProblemRequest(
            "Test Problem",
            "Test Description"
        );

        Problem savedProblem = new Problem(
            ProblemId.generate(),
            TEST_ORG_ID,
            request.getTitle(),
            request.getDescription(),
            TEST_USER_ID
        );

        when(problemRepository.save(any(Problem.class))).thenReturn(savedProblem);

        // When
        problemService.createProblem(request, TEST_USER_ID);

        // Then
        ArgumentCaptor<ProblemCreated> eventCaptor = ArgumentCaptor.forClass(ProblemCreated.class);
        verify(eventPublisher).publish(eventCaptor.capture());

        ProblemCreated event = eventCaptor.getValue();
        assertEquals(savedProblem.getId().getValue(), event.getProblemId());
        assertEquals(TEST_ORG_ID, event.getOrganizationId());
    }

    @Test
    void shouldGetProblemById() {
        // Given
        ProblemId problemId = ProblemId.generate();
        Problem problem = new Problem(
            problemId,
            TEST_ORG_ID,
            "Test Problem",
            "Test Description",
            TEST_USER_ID
        );

        when(problemRepository.findById(problemId)).thenReturn(Optional.of(problem));

        // When
        ProblemDto result = problemService.getProblem(problemId);

        // Then
        assertNotNull(result);
        assertEquals(problemId.getValue(), result.getId());
        assertEquals(problem.getTitle(), result.getTitle());
        assertEquals(problem.getDescription(), result.getDescription());

        verify(problemRepository).findById(problemId);
    }

    @Test
    void shouldThrowExceptionWhenProblemNotFound() {
        // Given
        ProblemId problemId = ProblemId.generate();
        when(problemRepository.findById(problemId)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(ProblemNotFoundException.class, () -> {
            problemService.getProblem(problemId);
        });

        verify(problemRepository).findById(problemId);
    }

    @Test
    void shouldGetAllProblemsForOrganization() {
        // Given
        Problem problem1 = new Problem(
            ProblemId.generate(),
            TEST_ORG_ID,
            "Problem 1",
            "Description 1",
            TEST_USER_ID
        );
        Problem problem2 = new Problem(
            ProblemId.generate(),
            TEST_ORG_ID,
            "Problem 2",
            "Description 2",
            TEST_USER_ID
        );

        when(problemRepository.findByOrganizationId(TEST_ORG_ID))
            .thenReturn(List.of(problem1, problem2));

        // When
        List<ProblemDto> results = problemService.getAllProblems();

        // Then
        assertEquals(2, results.size());
        assertEquals("Problem 1", results.get(0).getTitle());
        assertEquals("Problem 2", results.get(1).getTitle());
        assertTrue(results.stream().allMatch(p -> p.getOrganizationId().equals(TEST_ORG_ID)));

        verify(problemRepository).findByOrganizationId(TEST_ORG_ID);
    }

    @Test
    void shouldReturnEmptyListWhenNoProblemsExist() {
        // Given
        when(problemRepository.findByOrganizationId(TEST_ORG_ID))
            .thenReturn(List.of());

        // When
        List<ProblemDto> results = problemService.getAllProblems();

        // Then
        assertTrue(results.isEmpty());
        verify(problemRepository).findByOrganizationId(TEST_ORG_ID);
    }

    @Test
    void shouldUpdateProblem() {
        // Given
        ProblemId problemId = ProblemId.generate();
        Problem existingProblem = new Problem(
            problemId,
            TEST_ORG_ID,
            "Original Title",
            "Original Description",
            TEST_USER_ID
        );

        UpdateProblemRequest request = new UpdateProblemRequest(
            "Updated Title",
            "Updated Description"
        );

        when(problemRepository.findById(problemId)).thenReturn(Optional.of(existingProblem));
        when(problemRepository.save(any(Problem.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        ProblemDto result = problemService.updateProblem(problemId, request);

        // Then
        assertNotNull(result);
        assertEquals("Updated Title", result.getTitle());
        assertEquals("Updated Description", result.getDescription());

        verify(problemRepository).findById(problemId);
        verify(problemRepository).save(any(Problem.class));
    }

    @Test
    void shouldThrowExceptionWhenUpdatingNonExistentProblem() {
        // Given
        ProblemId problemId = ProblemId.generate();
        UpdateProblemRequest request = new UpdateProblemRequest(
            "Updated Title",
            "Updated Description"
        );

        when(problemRepository.findById(problemId)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(ProblemNotFoundException.class, () -> {
            problemService.updateProblem(problemId, request);
        });

        verify(problemRepository).findById(problemId);
        verify(problemRepository, never()).save(any(Problem.class));
    }

    @Test
    void shouldDeleteProblem() {
        // Given
        ProblemId problemId = ProblemId.generate();
        Problem problem = new Problem(
            problemId,
            TEST_ORG_ID,
            "Test Problem",
            "Test Description",
            TEST_USER_ID
        );

        when(problemRepository.findById(problemId)).thenReturn(Optional.of(problem));
        doNothing().when(problemRepository).delete(problem);

        // When
        problemService.deleteProblem(problemId);

        // Then
        verify(problemRepository).findById(problemId);
        verify(problemRepository).delete(problem);
    }

    @Test
    void shouldThrowExceptionWhenDeletingNonExistentProblem() {
        // Given
        ProblemId problemId = ProblemId.generate();
        when(problemRepository.findById(problemId)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(ProblemNotFoundException.class, () -> {
            problemService.deleteProblem(problemId);
        });

        verify(problemRepository).findById(problemId);
        verify(problemRepository, never()).delete(any(Problem.class));
    }

    @Test
    void shouldUseTenantContextForOrganizationId() {
        // Given
        CreateProblemRequest request = new CreateProblemRequest(
            "Test Problem",
            "Test Description"
        );

        Problem savedProblem = new Problem(
            ProblemId.generate(),
            TEST_ORG_ID,
            request.getTitle(),
            request.getDescription(),
            TEST_USER_ID
        );

        when(problemRepository.save(any(Problem.class))).thenReturn(savedProblem);

        // When
        ProblemDto result = problemService.createProblem(request, TEST_USER_ID);

        // Then
        assertEquals(TEST_ORG_ID, result.getOrganizationId());

        ArgumentCaptor<Problem> problemCaptor = ArgumentCaptor.forClass(Problem.class);
        verify(problemRepository).save(problemCaptor.capture());
        assertEquals(TEST_ORG_ID, problemCaptor.getValue().getOrganizationId());
    }

    @Test
    void shouldClearDomainEventsAfterPublishing() {
        // Given
        CreateProblemRequest request = new CreateProblemRequest(
            "Test Problem",
            "Test Description"
        );

        Problem savedProblem = new Problem(
            ProblemId.generate(),
            TEST_ORG_ID,
            request.getTitle(),
            request.getDescription(),
            TEST_USER_ID
        );

        // Verify problem has events before save
        assertEquals(1, savedProblem.getDomainEvents().size());

        when(problemRepository.save(any(Problem.class))).thenReturn(savedProblem);

        // When
        problemService.createProblem(request, TEST_USER_ID);

        // Then
        verify(eventPublisher).publish(any(ProblemCreated.class));
        // Events should be cleared after publishing
        assertEquals(0, savedProblem.getDomainEvents().size());
    }
}
