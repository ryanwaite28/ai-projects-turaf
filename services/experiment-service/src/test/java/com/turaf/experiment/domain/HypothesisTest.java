package com.turaf.experiment.domain;

import com.turaf.experiment.domain.event.HypothesisCreated;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class HypothesisTest {

    @Test
    void shouldCreateHypothesisWithValidData() {
        // Given
        HypothesisId id = HypothesisId.generate();
        String organizationId = "org-123";
        ProblemId problemId = ProblemId.generate();
        String statement = "Implementing caching will reduce login time by 50%";
        String expectedOutcome = "Login time < 500ms";
        String createdBy = "user-456";

        // When
        Hypothesis hypothesis = new Hypothesis(id, organizationId, problemId, statement, expectedOutcome, createdBy);

        // Then
        assertEquals(id, hypothesis.getId());
        assertEquals(organizationId, hypothesis.getOrganizationId());
        assertEquals(problemId, hypothesis.getProblemId());
        assertEquals(statement, hypothesis.getStatement());
        assertEquals(expectedOutcome, hypothesis.getExpectedOutcome());
        assertEquals(createdBy, hypothesis.getCreatedBy());
        assertNotNull(hypothesis.getCreatedAt());
        assertNotNull(hypothesis.getUpdatedAt());
    }

    @Test
    void shouldRegisterHypothesisCreatedEvent() {
        // Given
        HypothesisId id = HypothesisId.generate();
        String organizationId = "org-123";
        ProblemId problemId = ProblemId.generate();
        String statement = "Test hypothesis";
        String expectedOutcome = "Expected outcome";
        String createdBy = "user-456";

        // When
        Hypothesis hypothesis = new Hypothesis(id, organizationId, problemId, statement, expectedOutcome, createdBy);

        // Then
        assertEquals(1, hypothesis.getDomainEvents().size());
        assertTrue(hypothesis.getDomainEvents().get(0) instanceof HypothesisCreated);
        
        HypothesisCreated event = (HypothesisCreated) hypothesis.getDomainEvents().get(0);
        assertEquals(id.getValue(), event.getHypothesisId());
        assertEquals(organizationId, event.getOrganizationId());
        assertEquals(problemId.getValue(), event.getProblemId());
        assertEquals(statement, event.getStatement());
    }

    @Test
    void shouldUpdateHypothesisStatementAndOutcome() {
        // Given
        Hypothesis hypothesis = new Hypothesis(
            HypothesisId.generate(),
            "org-123",
            ProblemId.generate(),
            "Original statement",
            "Original outcome",
            "user-456"
        );
        
        String newStatement = "Updated statement";
        String newOutcome = "Updated outcome";

        // When
        hypothesis.update(newStatement, newOutcome);

        // Then
        assertEquals(newStatement, hypothesis.getStatement());
        assertEquals(newOutcome, hypothesis.getExpectedOutcome());
    }

    @Test
    void shouldThrowExceptionWhenStatementIsNull() {
        // Given
        HypothesisId id = HypothesisId.generate();

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            new Hypothesis(id, "org-123", ProblemId.generate(), null, "Outcome", "user-456");
        });
    }

    @Test
    void shouldThrowExceptionWhenStatementIsEmpty() {
        // Given
        HypothesisId id = HypothesisId.generate();

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            new Hypothesis(id, "org-123", ProblemId.generate(), "", "Outcome", "user-456");
        });
    }

    @Test
    void shouldThrowExceptionWhenStatementIsTooLong() {
        // Given
        HypothesisId id = HypothesisId.generate();
        String longStatement = "a".repeat(501);

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            new Hypothesis(id, "org-123", ProblemId.generate(), longStatement, "Outcome", "user-456");
        });
    }

    @Test
    void shouldThrowExceptionWhenProblemIdIsNull() {
        // Given
        HypothesisId id = HypothesisId.generate();

        // When & Then
        assertThrows(NullPointerException.class, () -> {
            new Hypothesis(id, "org-123", null, "Statement", "Outcome", "user-456");
        });
    }

    @Test
    void shouldThrowExceptionWhenOrganizationIdIsNull() {
        // Given
        HypothesisId id = HypothesisId.generate();

        // When & Then
        assertThrows(NullPointerException.class, () -> {
            new Hypothesis(id, null, ProblemId.generate(), "Statement", "Outcome", "user-456");
        });
    }

    @Test
    void shouldImplementTenantAware() {
        // Given
        Hypothesis hypothesis = new Hypothesis(
            HypothesisId.generate(),
            "org-123",
            ProblemId.generate(),
            "Statement",
            "Outcome",
            "user-456"
        );

        // When
        String newOrgId = "org-999";
        hypothesis.setOrganizationId(newOrgId);

        // Then
        assertEquals(newOrgId, hypothesis.getOrganizationId());
    }
}
