package com.turaf.experiment.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.turaf.common.security.TenantContextHolder;
import com.turaf.experiment.application.dto.*;
import com.turaf.experiment.domain.*;
import com.turaf.experiment.infrastructure.persistence.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ExperimentControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ProblemJpaRepository problemJpaRepository;

    @Autowired
    private HypothesisJpaRepository hypothesisJpaRepository;

    @Autowired
    private ExperimentJpaRepository experimentJpaRepository;

    private static final String TEST_ORG_ID = "org-integration-test";
    private static final String TEST_USER_ID = "user-integration-test";

    @BeforeEach
    void setUp() {
        TenantContextHolder.setOrganizationId(TEST_ORG_ID);
        experimentJpaRepository.deleteAll();
        hypothesisJpaRepository.deleteAll();
        problemJpaRepository.deleteAll();
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
        experimentJpaRepository.deleteAll();
        hypothesisJpaRepository.deleteAll();
        problemJpaRepository.deleteAll();
    }

    @Test
    @WithMockUser(username = TEST_USER_ID)
    void shouldCreateProblem() throws Exception {
        // Given
        CreateProblemRequest request = new CreateProblemRequest(
            "User authentication is slow",
            "Users experiencing delays during login"
        );

        // When & Then
        mockMvc.perform(post("/api/v1/problems")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").exists())
            .andExpect(jsonPath("$.title").value("User authentication is slow"))
            .andExpect(jsonPath("$.description").value("Users experiencing delays during login"))
            .andExpect(jsonPath("$.organizationId").value(TEST_ORG_ID))
            .andExpect(jsonPath("$.createdBy").value(TEST_USER_ID));
    }

    @Test
    @WithMockUser(username = TEST_USER_ID)
    void shouldGetAllProblems() throws Exception {
        // Given
        Problem problem1 = createTestProblem("Problem 1", "Description 1");
        Problem problem2 = createTestProblem("Problem 2", "Description 2");

        // When & Then
        mockMvc.perform(get("/api/v1/problems"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(2)))
            .andExpect(jsonPath("$[*].title", containsInAnyOrder("Problem 1", "Problem 2")));
    }

    @Test
    @WithMockUser(username = TEST_USER_ID)
    void shouldGetProblemById() throws Exception {
        // Given
        Problem problem = createTestProblem("Test Problem", "Test Description");

        // When & Then
        mockMvc.perform(get("/api/v1/problems/" + problem.getId().getValue()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(problem.getId().getValue()))
            .andExpect(jsonPath("$.title").value("Test Problem"));
    }

    @Test
    @WithMockUser(username = TEST_USER_ID)
    void shouldUpdateProblem() throws Exception {
        // Given
        Problem problem = createTestProblem("Original Title", "Original Description");
        UpdateProblemRequest request = new UpdateProblemRequest(
            "Updated Title",
            "Updated Description"
        );

        // When & Then
        mockMvc.perform(put("/api/v1/problems/" + problem.getId().getValue())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.title").value("Updated Title"))
            .andExpect(jsonPath("$.description").value("Updated Description"));
    }

    @Test
    @WithMockUser(username = TEST_USER_ID)
    void shouldDeleteProblem() throws Exception {
        // Given
        Problem problem = createTestProblem("Test Problem", "Test Description");

        // When & Then
        mockMvc.perform(delete("/api/v1/problems/" + problem.getId().getValue()))
            .andExpect(status().isNoContent());

        // Verify deleted
        mockMvc.perform(get("/api/v1/problems/" + problem.getId().getValue()))
            .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = TEST_USER_ID)
    void shouldCreateHypothesis() throws Exception {
        // Given
        Problem problem = createTestProblem("Test Problem", "Test Description");
        CreateHypothesisRequest request = new CreateHypothesisRequest(
            problem.getId().getValue(),
            "Implementing caching will reduce login time",
            "Login time < 500ms"
        );

        // When & Then
        mockMvc.perform(post("/api/v1/hypotheses")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").exists())
            .andExpect(jsonPath("$.problemId").value(problem.getId().getValue()))
            .andExpect(jsonPath("$.statement").value("Implementing caching will reduce login time"))
            .andExpect(jsonPath("$.expectedOutcome").value("Login time < 500ms"));
    }

    @Test
    @WithMockUser(username = TEST_USER_ID)
    void shouldGetHypothesesByProblem() throws Exception {
        // Given
        Problem problem = createTestProblem("Test Problem", "Test Description");
        Hypothesis hypothesis1 = createTestHypothesis(problem.getId(), "Hypothesis 1", "Outcome 1");
        Hypothesis hypothesis2 = createTestHypothesis(problem.getId(), "Hypothesis 2", "Outcome 2");

        // When & Then
        mockMvc.perform(get("/api/v1/hypotheses")
                .param("problemId", problem.getId().getValue()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(2)))
            .andExpect(jsonPath("$[*].statement", containsInAnyOrder("Hypothesis 1", "Hypothesis 2")));
    }

    @Test
    @WithMockUser(username = TEST_USER_ID)
    void shouldCreateExperiment() throws Exception {
        // Given
        Problem problem = createTestProblem("Test Problem", "Test Description");
        Hypothesis hypothesis = createTestHypothesis(problem.getId(), "Test Hypothesis", "Test Outcome");
        CreateExperimentRequest request = new CreateExperimentRequest(
            hypothesis.getId().getValue(),
            "Cache Implementation Test",
            "Testing Redis caching"
        );

        // When & Then
        mockMvc.perform(post("/api/v1/experiments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").exists())
            .andExpect(jsonPath("$.hypothesisId").value(hypothesis.getId().getValue()))
            .andExpect(jsonPath("$.name").value("Cache Implementation Test"))
            .andExpect(jsonPath("$.status").value("DRAFT"))
            .andExpect(jsonPath("$.allowedTransitions", hasSize(2)))
            .andExpect(jsonPath("$.allowedTransitions", containsInAnyOrder("RUNNING", "CANCELLED")));
    }

    @Test
    @WithMockUser(username = TEST_USER_ID)
    void shouldStartExperiment() throws Exception {
        // Given
        Problem problem = createTestProblem("Test Problem", "Test Description");
        Hypothesis hypothesis = createTestHypothesis(problem.getId(), "Test Hypothesis", "Test Outcome");
        Experiment experiment = createTestExperiment(hypothesis.getId(), "Test Experiment", "Test Description");

        // When & Then
        mockMvc.perform(post("/api/v1/experiments/" + experiment.getId().getValue() + "/start"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("RUNNING"))
            .andExpect(jsonPath("$.startedAt").exists())
            .andExpect(jsonPath("$.allowedTransitions", hasSize(2)))
            .andExpect(jsonPath("$.allowedTransitions", containsInAnyOrder("COMPLETED", "CANCELLED")));
    }

    @Test
    @WithMockUser(username = TEST_USER_ID)
    void shouldCompleteExperiment() throws Exception {
        // Given
        Problem problem = createTestProblem("Test Problem", "Test Description");
        Hypothesis hypothesis = createTestHypothesis(problem.getId(), "Test Hypothesis", "Test Outcome");
        Experiment experiment = createTestExperiment(hypothesis.getId(), "Test Experiment", "Test Description");
        experiment.start();
        experimentJpaRepository.save(ExperimentJpaEntity.fromDomain(experiment));

        // When & Then
        mockMvc.perform(post("/api/v1/experiments/" + experiment.getId().getValue() + "/complete"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("COMPLETED"))
            .andExpect(jsonPath("$.completedAt").exists())
            .andExpect(jsonPath("$.allowedTransitions", hasSize(0)));
    }

    @Test
    @WithMockUser(username = TEST_USER_ID)
    void shouldCancelExperiment() throws Exception {
        // Given
        Problem problem = createTestProblem("Test Problem", "Test Description");
        Hypothesis hypothesis = createTestHypothesis(problem.getId(), "Test Hypothesis", "Test Outcome");
        Experiment experiment = createTestExperiment(hypothesis.getId(), "Test Experiment", "Test Description");

        // When & Then
        mockMvc.perform(post("/api/v1/experiments/" + experiment.getId().getValue() + "/cancel"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("CANCELLED"))
            .andExpect(jsonPath("$.allowedTransitions", hasSize(0)));
    }

    @Test
    @WithMockUser(username = TEST_USER_ID)
    void shouldRejectInvalidStateTransition() throws Exception {
        // Given - Create experiment in DRAFT state
        Problem problem = createTestProblem("Test Problem", "Test Description");
        Hypothesis hypothesis = createTestHypothesis(problem.getId(), "Test Hypothesis", "Test Outcome");
        Experiment experiment = createTestExperiment(hypothesis.getId(), "Test Experiment", "Test Description");

        // When & Then - Try to complete without starting
        mockMvc.perform(post("/api/v1/experiments/" + experiment.getId().getValue() + "/complete"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("INVALID_STATE_TRANSITION"));
    }

    @Test
    @WithMockUser(username = TEST_USER_ID)
    void shouldGetExperimentsByStatus() throws Exception {
        // Given
        Problem problem = createTestProblem("Test Problem", "Test Description");
        Hypothesis hypothesis = createTestHypothesis(problem.getId(), "Test Hypothesis", "Test Outcome");
        Experiment draftExperiment = createTestExperiment(hypothesis.getId(), "Draft Experiment", "Description");
        Experiment runningExperiment = createTestExperiment(hypothesis.getId(), "Running Experiment", "Description");
        runningExperiment.start();
        experimentJpaRepository.save(ExperimentJpaEntity.fromDomain(runningExperiment));

        // When & Then
        mockMvc.perform(get("/api/v1/experiments")
                .param("status", "DRAFT"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].status").value("DRAFT"));

        mockMvc.perform(get("/api/v1/experiments")
                .param("status", "RUNNING"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].status").value("RUNNING"));
    }

    @Test
    @WithMockUser(username = TEST_USER_ID)
    void shouldGetExperimentsByHypothesis() throws Exception {
        // Given
        Problem problem = createTestProblem("Test Problem", "Test Description");
        Hypothesis hypothesis = createTestHypothesis(problem.getId(), "Test Hypothesis", "Test Outcome");
        Experiment experiment1 = createTestExperiment(hypothesis.getId(), "Experiment 1", "Description 1");
        Experiment experiment2 = createTestExperiment(hypothesis.getId(), "Experiment 2", "Description 2");

        // When & Then
        mockMvc.perform(get("/api/v1/experiments")
                .param("hypothesisId", hypothesis.getId().getValue()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(2)))
            .andExpect(jsonPath("$[*].name", containsInAnyOrder("Experiment 1", "Experiment 2")));
    }

    @Test
    @WithMockUser(username = TEST_USER_ID)
    void shouldReturn404ForNonExistentProblem() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/problems/non-existent-id"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("PROBLEM_NOT_FOUND"));
    }

    @Test
    @WithMockUser(username = TEST_USER_ID)
    void shouldReturn404ForNonExistentHypothesis() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/hypotheses/non-existent-id"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("HYPOTHESIS_NOT_FOUND"));
    }

    @Test
    @WithMockUser(username = TEST_USER_ID)
    void shouldReturn404ForNonExistentExperiment() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/experiments/non-existent-id"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("EXPERIMENT_NOT_FOUND"));
    }

    @Test
    @WithMockUser(username = TEST_USER_ID)
    void shouldValidateCreateProblemRequest() throws Exception {
        // Given - Invalid request with blank title
        CreateProblemRequest request = new CreateProblemRequest("", "Description");

        // When & Then
        mockMvc.perform(post("/api/v1/problems")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

    // Helper methods
    private Problem createTestProblem(String title, String description) {
        Problem problem = new Problem(
            ProblemId.generate(),
            TEST_ORG_ID,
            title,
            description,
            TEST_USER_ID
        );
        problem.clearDomainEvents();
        problemJpaRepository.save(ProblemJpaEntity.fromDomain(problem));
        return problem;
    }

    private Hypothesis createTestHypothesis(ProblemId problemId, String statement, String outcome) {
        Hypothesis hypothesis = new Hypothesis(
            HypothesisId.generate(),
            TEST_ORG_ID,
            problemId,
            statement,
            outcome,
            TEST_USER_ID
        );
        hypothesis.clearDomainEvents();
        hypothesisJpaRepository.save(HypothesisJpaEntity.fromDomain(hypothesis));
        return hypothesis;
    }

    private Experiment createTestExperiment(HypothesisId hypothesisId, String name, String description) {
        Experiment experiment = new Experiment(
            ExperimentId.generate(),
            TEST_ORG_ID,
            hypothesisId,
            name,
            description,
            TEST_USER_ID
        );
        experiment.clearDomainEvents();
        experimentJpaRepository.save(ExperimentJpaEntity.fromDomain(experiment));
        return experiment;
    }
}
