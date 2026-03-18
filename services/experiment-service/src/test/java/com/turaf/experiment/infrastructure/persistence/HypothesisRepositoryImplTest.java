package com.turaf.experiment.infrastructure.persistence;

import com.turaf.experiment.domain.Hypothesis;
import com.turaf.experiment.domain.HypothesisId;
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
@Import({HypothesisRepositoryImpl.class, ProblemRepositoryImpl.class})
class HypothesisRepositoryImplTest {

    @Autowired
    private HypothesisRepositoryImpl hypothesisRepository;

    @Autowired
    private ProblemRepositoryImpl problemRepository;

    @Autowired
    private HypothesisJpaRepository jpaRepository;

    private ProblemId problemId;

    @BeforeEach
    void setUp() {
        jpaRepository.deleteAll();
        
        // Create a problem for foreign key constraint
        Problem problem = new Problem(
            ProblemId.generate(),
            "org-123",
            "Test Problem",
            "Test Description",
            "user-456"
        );
        Problem savedProblem = problemRepository.save(problem);
        problemId = savedProblem.getId();
    }

    @Test
    void shouldSaveAndRetrieveHypothesis() {
        // Given
        Hypothesis hypothesis = new Hypothesis(
            HypothesisId.generate(),
            "org-123",
            problemId,
            "Implementing caching will reduce login time by 50%",
            "Login time < 500ms",
            "user-456"
        );

        // When
        Hypothesis saved = hypothesisRepository.save(hypothesis);
        Optional<Hypothesis> retrieved = hypothesisRepository.findById(saved.getId());

        // Then
        assertTrue(retrieved.isPresent());
        assertEquals(saved.getId(), retrieved.get().getId());
        assertEquals(saved.getStatement(), retrieved.get().getStatement());
        assertEquals(saved.getProblemId(), retrieved.get().getProblemId());
    }

    @Test
    void shouldFindHypothesesByProblemId() {
        // Given
        Hypothesis hypothesis1 = new Hypothesis(
            HypothesisId.generate(),
            "org-123",
            problemId,
            "Hypothesis 1",
            "Outcome 1",
            "user-1"
        );
        Hypothesis hypothesis2 = new Hypothesis(
            HypothesisId.generate(),
            "org-123",
            problemId,
            "Hypothesis 2",
            "Outcome 2",
            "user-1"
        );

        hypothesisRepository.save(hypothesis1);
        hypothesisRepository.save(hypothesis2);

        // When
        List<Hypothesis> hypotheses = hypothesisRepository.findByProblemId(problemId);

        // Then
        assertEquals(2, hypotheses.size());
        assertTrue(hypotheses.stream().allMatch(h -> h.getProblemId().equals(problemId)));
    }

    @Test
    void shouldFindHypothesesByOrganizationId() {
        // Given
        String orgId = "org-123";
        Hypothesis hypothesis1 = new Hypothesis(
            HypothesisId.generate(),
            orgId,
            problemId,
            "Hypothesis 1",
            "Outcome 1",
            "user-1"
        );
        Hypothesis hypothesis2 = new Hypothesis(
            HypothesisId.generate(),
            orgId,
            problemId,
            "Hypothesis 2",
            "Outcome 2",
            "user-1"
        );

        hypothesisRepository.save(hypothesis1);
        hypothesisRepository.save(hypothesis2);

        // When
        List<Hypothesis> hypotheses = hypothesisRepository.findByOrganizationId(orgId);

        // Then
        assertEquals(2, hypotheses.size());
        assertTrue(hypotheses.stream().allMatch(h -> h.getOrganizationId().equals(orgId)));
    }

    @Test
    void shouldUpdateExistingHypothesis() {
        // Given
        Hypothesis hypothesis = new Hypothesis(
            HypothesisId.generate(),
            "org-123",
            problemId,
            "Original Statement",
            "Original Outcome",
            "user-456"
        );
        Hypothesis saved = hypothesisRepository.save(hypothesis);

        // When
        saved.update("Updated Statement", "Updated Outcome");
        Hypothesis updated = hypothesisRepository.save(saved);
        Optional<Hypothesis> retrieved = hypothesisRepository.findById(updated.getId());

        // Then
        assertTrue(retrieved.isPresent());
        assertEquals("Updated Statement", retrieved.get().getStatement());
        assertEquals("Updated Outcome", retrieved.get().getExpectedOutcome());
    }

    @Test
    void shouldDeleteHypothesis() {
        // Given
        Hypothesis hypothesis = new Hypothesis(
            HypothesisId.generate(),
            "org-123",
            problemId,
            "Test Hypothesis",
            "Test Outcome",
            "user-456"
        );
        Hypothesis saved = hypothesisRepository.save(hypothesis);

        // When
        hypothesisRepository.delete(saved);
        Optional<Hypothesis> retrieved = hypothesisRepository.findById(saved.getId());

        // Then
        assertFalse(retrieved.isPresent());
    }

    @Test
    void shouldCascadeDeleteWhenProblemIsDeleted() {
        // Given
        Hypothesis hypothesis = new Hypothesis(
            HypothesisId.generate(),
            "org-123",
            problemId,
            "Test Hypothesis",
            "Test Outcome",
            "user-456"
        );
        Hypothesis saved = hypothesisRepository.save(hypothesis);

        // When
        Optional<Problem> problem = problemRepository.findById(problemId);
        assertTrue(problem.isPresent());
        problemRepository.delete(problem.get());

        // Then
        Optional<Hypothesis> retrieved = hypothesisRepository.findById(saved.getId());
        assertFalse(retrieved.isPresent());
    }
}
