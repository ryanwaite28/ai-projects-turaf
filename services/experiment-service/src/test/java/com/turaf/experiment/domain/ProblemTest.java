package com.turaf.experiment.domain;

import com.turaf.experiment.domain.event.ProblemCreated;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ProblemTest {

    @Test
    void shouldCreateProblemWithValidData() {
        // Given
        ProblemId id = ProblemId.generate();
        String organizationId = "org-123";
        String title = "User authentication is slow";
        String description = "Users are experiencing slow login times";
        String createdBy = "user-456";

        // When
        Problem problem = new Problem(id, organizationId, title, description, createdBy);

        // Then
        assertEquals(id, problem.getId());
        assertEquals(organizationId, problem.getOrganizationId());
        assertEquals(title, problem.getTitle());
        assertEquals(description, problem.getDescription());
        assertEquals(createdBy, problem.getCreatedBy());
        assertNotNull(problem.getCreatedAt());
        assertNotNull(problem.getUpdatedAt());
    }

    @Test
    void shouldRegisterProblemCreatedEvent() {
        // Given
        ProblemId id = ProblemId.generate();
        String organizationId = "org-123";
        String title = "Test Problem";
        String description = "Test Description";
        String createdBy = "user-456";

        // When
        Problem problem = new Problem(id, organizationId, title, description, createdBy);

        // Then
        assertEquals(1, problem.getDomainEvents().size());
        assertTrue(problem.getDomainEvents().get(0) instanceof ProblemCreated);
        
        ProblemCreated event = (ProblemCreated) problem.getDomainEvents().get(0);
        assertEquals(id.getValue(), event.getProblemId());
        assertEquals(organizationId, event.getOrganizationId());
        assertEquals(title, event.getTitle());
    }

    @Test
    void shouldUpdateProblemTitleAndDescription() {
        // Given
        Problem problem = new Problem(
            ProblemId.generate(),
            "org-123",
            "Original Title",
            "Original Description",
            "user-456"
        );
        
        String newTitle = "Updated Title";
        String newDescription = "Updated Description";

        // When
        problem.update(newTitle, newDescription);

        // Then
        assertEquals(newTitle, problem.getTitle());
        assertEquals(newDescription, problem.getDescription());
    }

    @Test
    void shouldThrowExceptionWhenTitleIsNull() {
        // Given
        ProblemId id = ProblemId.generate();

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            new Problem(id, "org-123", null, "Description", "user-456");
        });
    }

    @Test
    void shouldThrowExceptionWhenTitleIsEmpty() {
        // Given
        ProblemId id = ProblemId.generate();

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            new Problem(id, "org-123", "", "Description", "user-456");
        });
    }

    @Test
    void shouldThrowExceptionWhenTitleIsTooLong() {
        // Given
        ProblemId id = ProblemId.generate();
        String longTitle = "a".repeat(201);

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            new Problem(id, "org-123", longTitle, "Description", "user-456");
        });
    }

    @Test
    void shouldThrowExceptionWhenOrganizationIdIsNull() {
        // Given
        ProblemId id = ProblemId.generate();

        // When & Then
        assertThrows(NullPointerException.class, () -> {
            new Problem(id, null, "Title", "Description", "user-456");
        });
    }

    @Test
    void shouldThrowExceptionWhenCreatedByIsNull() {
        // Given
        ProblemId id = ProblemId.generate();

        // When & Then
        assertThrows(NullPointerException.class, () -> {
            new Problem(id, "org-123", "Title", "Description", null);
        });
    }

    @Test
    void shouldImplementTenantAware() {
        // Given
        Problem problem = new Problem(
            ProblemId.generate(),
            "org-123",
            "Title",
            "Description",
            "user-456"
        );

        // When
        String newOrgId = "org-999";
        problem.setOrganizationId(newOrgId);

        // Then
        assertEquals(newOrgId, problem.getOrganizationId());
    }
}
