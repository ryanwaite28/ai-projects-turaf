package com.turaf.experiment.infrastructure.persistence;

import com.turaf.experiment.domain.Problem;
import com.turaf.experiment.domain.ProblemId;
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
@Import(ProblemRepositoryImpl.class)
class ProblemRepositoryImplTest {

    @Autowired
    private ProblemRepositoryImpl problemRepository;

    @Autowired
    private ProblemJpaRepository jpaRepository;

    @BeforeEach
    void setUp() {
        jpaRepository.deleteAll();
    }

    @Test
    void shouldSaveAndRetrieveProblem() {
        // Given
        Problem problem = new Problem(
            ProblemId.generate(),
            "org-123",
            "User authentication is slow",
            "Users experiencing delays during login",
            "user-456"
        );

        // When
        Problem saved = problemRepository.save(problem);
        Optional<Problem> retrieved = problemRepository.findById(saved.getId());

        // Then
        assertTrue(retrieved.isPresent());
        assertEquals(saved.getId(), retrieved.get().getId());
        assertEquals(saved.getTitle(), retrieved.get().getTitle());
        assertEquals(saved.getOrganizationId(), retrieved.get().getOrganizationId());
    }

    @Test
    void shouldFindProblemsByOrganizationId() {
        // Given
        String orgId = "org-123";
        Problem problem1 = new Problem(ProblemId.generate(), orgId, "Problem 1", "Description 1", "user-1");
        Problem problem2 = new Problem(ProblemId.generate(), orgId, "Problem 2", "Description 2", "user-1");
        Problem problem3 = new Problem(ProblemId.generate(), "org-999", "Problem 3", "Description 3", "user-2");

        problemRepository.save(problem1);
        problemRepository.save(problem2);
        problemRepository.save(problem3);

        // When
        List<Problem> problems = problemRepository.findByOrganizationId(orgId);

        // Then
        assertEquals(2, problems.size());
        assertTrue(problems.stream().allMatch(p -> p.getOrganizationId().equals(orgId)));
    }

    @Test
    void shouldReturnEmptyWhenProblemNotFound() {
        // Given
        ProblemId nonExistentId = ProblemId.generate();

        // When
        Optional<Problem> result = problemRepository.findById(nonExistentId);

        // Then
        assertFalse(result.isPresent());
    }

    @Test
    void shouldUpdateExistingProblem() {
        // Given
        Problem problem = new Problem(
            ProblemId.generate(),
            "org-123",
            "Original Title",
            "Original Description",
            "user-456"
        );
        Problem saved = problemRepository.save(problem);

        // When
        saved.update("Updated Title", "Updated Description");
        Problem updated = problemRepository.save(saved);
        Optional<Problem> retrieved = problemRepository.findById(updated.getId());

        // Then
        assertTrue(retrieved.isPresent());
        assertEquals("Updated Title", retrieved.get().getTitle());
        assertEquals("Updated Description", retrieved.get().getDescription());
    }

    @Test
    void shouldDeleteProblem() {
        // Given
        Problem problem = new Problem(
            ProblemId.generate(),
            "org-123",
            "Test Problem",
            "Test Description",
            "user-456"
        );
        Problem saved = problemRepository.save(problem);

        // When
        problemRepository.delete(saved);
        Optional<Problem> retrieved = problemRepository.findById(saved.getId());

        // Then
        assertFalse(retrieved.isPresent());
    }

    @Test
    void shouldFindAllProblems() {
        // Given
        Problem problem1 = new Problem(ProblemId.generate(), "org-123", "Problem 1", "Desc 1", "user-1");
        Problem problem2 = new Problem(ProblemId.generate(), "org-456", "Problem 2", "Desc 2", "user-2");

        problemRepository.save(problem1);
        problemRepository.save(problem2);

        // When
        List<Problem> allProblems = problemRepository.findAll();

        // Then
        assertEquals(2, allProblems.size());
    }

    @Test
    void shouldPreserveDomainEventsClearedAfterRetrieval() {
        // Given
        Problem problem = new Problem(
            ProblemId.generate(),
            "org-123",
            "Test Problem",
            "Test Description",
            "user-456"
        );
        
        // Domain events are registered on creation
        assertEquals(1, problem.getDomainEvents().size());

        // When
        Problem saved = problemRepository.save(problem);
        Optional<Problem> retrieved = problemRepository.findById(saved.getId());

        // Then
        assertTrue(retrieved.isPresent());
        // Events should be cleared after retrieval from database
        assertEquals(0, retrieved.get().getDomainEvents().size());
    }
}
