package com.turaf.experiment.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.turaf.common.event.EventPublisher;
import com.turaf.common.tenant.TenantContextHolder;
import com.turaf.experiment.application.dto.*;
import com.turaf.experiment.domain.event.*;
import com.turaf.experiment.infrastructure.persistence.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ExperimentFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private EventPublisher eventPublisher;

    @Autowired
    private ProblemJpaRepository problemJpaRepository;

    @Autowired
    private HypothesisJpaRepository hypothesisJpaRepository;

    @Autowired
    private ExperimentJpaRepository experimentJpaRepository;

    private static final String TEST_ORG_ID = "org-flow-test";
    private static final String TEST_USER_ID = "user-flow-test";

    @BeforeEach
    void setUp() {
        TenantContextHolder.setOrganizationId(TEST_ORG_ID);
        experimentJpaRepository.deleteAll();
        hypothesisJpaRepository.deleteAll();
        problemJpaRepository.deleteAll();
        reset(eventPublisher);
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
    void shouldCompleteFullExperimentLifecycle() throws Exception {
        // Step 1: Create a problem
        CreateProblemRequest problemRequest = new CreateProblemRequest(
            "User authentication is slow",
            "Users experiencing delays during login"
        );

        MvcResult problemResult = mockMvc.perform(post("/api/v1/problems")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(problemRequest)))
            .andExpect(status().isCreated())
            .andReturn();

        ProblemDto problem = objectMapper.readValue(
            problemResult.getResponse().getContentAsString(),
            ProblemDto.class
        );

        // Verify ProblemCreated event was published
        ArgumentCaptor<ProblemCreated> problemEventCaptor = ArgumentCaptor.forClass(ProblemCreated.class);
        verify(eventPublisher, times(1)).publish(problemEventCaptor.capture());
        ProblemCreated problemEvent = problemEventCaptor.getValue();
        assertEquals(problem.getId(), problemEvent.getProblemId());
        assertEquals(TEST_ORG_ID, problemEvent.getOrganizationId());

        // Step 2: Create a hypothesis for the problem
        CreateHypothesisRequest hypothesisRequest = new CreateHypothesisRequest(
            problem.getId(),
            "Implementing Redis caching will reduce login time by 50%",
            "Login time < 500ms"
        );

        MvcResult hypothesisResult = mockMvc.perform(post("/api/v1/hypotheses")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(hypothesisRequest)))
            .andExpect(status().isCreated())
            .andReturn();

        HypothesisDto hypothesis = objectMapper.readValue(
            hypothesisResult.getResponse().getContentAsString(),
            HypothesisDto.class
        );

        // Verify HypothesisCreated event was published
        ArgumentCaptor<HypothesisCreated> hypothesisEventCaptor = ArgumentCaptor.forClass(HypothesisCreated.class);
        verify(eventPublisher, times(2)).publish(any());
        assertEquals(problem.getId(), hypothesis.getProblemId());

        // Step 3: Create an experiment for the hypothesis
        CreateExperimentRequest experimentRequest = new CreateExperimentRequest(
            hypothesis.getId(),
            "Redis Cache Implementation Test",
            "Testing Redis caching on authentication endpoints"
        );

        MvcResult experimentResult = mockMvc.perform(post("/api/v1/experiments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(experimentRequest)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value("DRAFT"))
            .andReturn();

        ExperimentDto experiment = objectMapper.readValue(
            experimentResult.getResponse().getContentAsString(),
            ExperimentDto.class
        );

        // Verify ExperimentCreated event was published
        verify(eventPublisher, times(3)).publish(any());
        assertEquals(hypothesis.getId(), experiment.getHypothesisId());
        assertEquals("DRAFT", experiment.getStatus());
        assertTrue(experiment.getAllowedTransitions().contains("RUNNING"));
        assertTrue(experiment.getAllowedTransitions().contains("CANCELLED"));

        // Step 4: Start the experiment
        MvcResult startResult = mockMvc.perform(post("/api/v1/experiments/" + experiment.getId() + "/start"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("RUNNING"))
            .andExpect(jsonPath("$.startedAt").exists())
            .andReturn();

        ExperimentDto runningExperiment = objectMapper.readValue(
            startResult.getResponse().getContentAsString(),
            ExperimentDto.class
        );

        // Verify ExperimentStarted event was published
        ArgumentCaptor<ExperimentStarted> startedEventCaptor = ArgumentCaptor.forClass(ExperimentStarted.class);
        verify(eventPublisher, times(4)).publish(any());
        assertEquals("RUNNING", runningExperiment.getStatus());
        assertNotNull(runningExperiment.getStartedAt());
        assertTrue(runningExperiment.getAllowedTransitions().contains("COMPLETED"));
        assertTrue(runningExperiment.getAllowedTransitions().contains("CANCELLED"));

        // Step 5: Complete the experiment
        MvcResult completeResult = mockMvc.perform(post("/api/v1/experiments/" + experiment.getId() + "/complete"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("COMPLETED"))
            .andExpect(jsonPath("$.completedAt").exists())
            .andReturn();

        ExperimentDto completedExperiment = objectMapper.readValue(
            completeResult.getResponse().getContentAsString(),
            ExperimentDto.class
        );

        // Verify ExperimentCompleted event was published
        ArgumentCaptor<ExperimentCompleted> completedEventCaptor = ArgumentCaptor.forClass(ExperimentCompleted.class);
        verify(eventPublisher, times(5)).publish(any());
        assertEquals("COMPLETED", completedExperiment.getStatus());
        assertNotNull(completedExperiment.getCompletedAt());
        assertTrue(completedExperiment.getAllowedTransitions().isEmpty());

        // Step 6: Verify the complete flow by retrieving all entities
        mockMvc.perform(get("/api/v1/problems/" + problem.getId()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.title").value("User authentication is slow"));

        mockMvc.perform(get("/api/v1/hypotheses/" + hypothesis.getId()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.problemId").value(problem.getId()));

        mockMvc.perform(get("/api/v1/experiments/" + experiment.getId()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    @WithMockUser(username = TEST_USER_ID)
    void shouldCancelExperimentInDraftState() throws Exception {
        // Given - Create problem, hypothesis, and experiment
        CreateProblemRequest problemRequest = new CreateProblemRequest("Test Problem", "Description");
        MvcResult problemResult = mockMvc.perform(post("/api/v1/problems")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(problemRequest)))
            .andExpect(status().isCreated())
            .andReturn();
        ProblemDto problem = objectMapper.readValue(problemResult.getResponse().getContentAsString(), ProblemDto.class);

        CreateHypothesisRequest hypothesisRequest = new CreateHypothesisRequest(
            problem.getId(), "Test Hypothesis", "Test Outcome"
        );
        MvcResult hypothesisResult = mockMvc.perform(post("/api/v1/hypotheses")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(hypothesisRequest)))
            .andExpect(status().isCreated())
            .andReturn();
        HypothesisDto hypothesis = objectMapper.readValue(hypothesisResult.getResponse().getContentAsString(), HypothesisDto.class);

        CreateExperimentRequest experimentRequest = new CreateExperimentRequest(
            hypothesis.getId(), "Test Experiment", "Description"
        );
        MvcResult experimentResult = mockMvc.perform(post("/api/v1/experiments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(experimentRequest)))
            .andExpect(status().isCreated())
            .andReturn();
        ExperimentDto experiment = objectMapper.readValue(experimentResult.getResponse().getContentAsString(), ExperimentDto.class);

        // When - Cancel the experiment in DRAFT state
        mockMvc.perform(post("/api/v1/experiments/" + experiment.getId() + "/cancel"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("CANCELLED"))
            .andExpect(jsonPath("$.allowedTransitions").isEmpty());

        // Then - Verify ExperimentCancelled event was published
        verify(eventPublisher, times(4)).publish(any());
    }

    @Test
    @WithMockUser(username = TEST_USER_ID)
    void shouldCancelExperimentInRunningState() throws Exception {
        // Given - Create and start an experiment
        CreateProblemRequest problemRequest = new CreateProblemRequest("Test Problem", "Description");
        MvcResult problemResult = mockMvc.perform(post("/api/v1/problems")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(problemRequest)))
            .andExpect(status().isCreated())
            .andReturn();
        ProblemDto problem = objectMapper.readValue(problemResult.getResponse().getContentAsString(), ProblemDto.class);

        CreateHypothesisRequest hypothesisRequest = new CreateHypothesisRequest(
            problem.getId(), "Test Hypothesis", "Test Outcome"
        );
        MvcResult hypothesisResult = mockMvc.perform(post("/api/v1/hypotheses")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(hypothesisRequest)))
            .andExpect(status().isCreated())
            .andReturn();
        HypothesisDto hypothesis = objectMapper.readValue(hypothesisResult.getResponse().getContentAsString(), HypothesisDto.class);

        CreateExperimentRequest experimentRequest = new CreateExperimentRequest(
            hypothesis.getId(), "Test Experiment", "Description"
        );
        MvcResult experimentResult = mockMvc.perform(post("/api/v1/experiments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(experimentRequest)))
            .andExpect(status().isCreated())
            .andReturn();
        ExperimentDto experiment = objectMapper.readValue(experimentResult.getResponse().getContentAsString(), ExperimentDto.class);

        // Start the experiment
        mockMvc.perform(post("/api/v1/experiments/" + experiment.getId() + "/start"))
            .andExpect(status().isOk());

        // When - Cancel the running experiment
        mockMvc.perform(post("/api/v1/experiments/" + experiment.getId() + "/cancel"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("CANCELLED"));

        // Then - Verify events were published
        verify(eventPublisher, times(5)).publish(any());
    }

    @Test
    @WithMockUser(username = TEST_USER_ID)
    void shouldEnforceTenantIsolation() throws Exception {
        // Given - Create entities in one tenant
        CreateProblemRequest problemRequest = new CreateProblemRequest("Test Problem", "Description");
        MvcResult problemResult = mockMvc.perform(post("/api/v1/problems")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(problemRequest)))
            .andExpect(status().isCreated())
            .andReturn();
        ProblemDto problem = objectMapper.readValue(problemResult.getResponse().getContentAsString(), ProblemDto.class);

        // Verify problem belongs to correct tenant
        assertEquals(TEST_ORG_ID, problem.getOrganizationId());

        // When - Switch to different tenant context
        TenantContextHolder.setOrganizationId("different-org");

        // Then - Should not see problems from other tenant
        mockMvc.perform(get("/api/v1/problems"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isEmpty());

        // Cleanup
        TenantContextHolder.setOrganizationId(TEST_ORG_ID);
    }

    @Test
    @WithMockUser(username = TEST_USER_ID)
    void shouldCascadeDeleteFromProblemToExperiments() throws Exception {
        // Given - Create complete hierarchy
        CreateProblemRequest problemRequest = new CreateProblemRequest("Test Problem", "Description");
        MvcResult problemResult = mockMvc.perform(post("/api/v1/problems")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(problemRequest)))
            .andExpect(status().isCreated())
            .andReturn();
        ProblemDto problem = objectMapper.readValue(problemResult.getResponse().getContentAsString(), ProblemDto.class);

        CreateHypothesisRequest hypothesisRequest = new CreateHypothesisRequest(
            problem.getId(), "Test Hypothesis", "Test Outcome"
        );
        MvcResult hypothesisResult = mockMvc.perform(post("/api/v1/hypotheses")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(hypothesisRequest)))
            .andExpect(status().isCreated())
            .andReturn();
        HypothesisDto hypothesis = objectMapper.readValue(hypothesisResult.getResponse().getContentAsString(), HypothesisDto.class);

        CreateExperimentRequest experimentRequest = new CreateExperimentRequest(
            hypothesis.getId(), "Test Experiment", "Description"
        );
        MvcResult experimentResult = mockMvc.perform(post("/api/v1/experiments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(experimentRequest)))
            .andExpect(status().isCreated())
            .andReturn();
        ExperimentDto experiment = objectMapper.readValue(experimentResult.getResponse().getContentAsString(), ExperimentDto.class);

        // When - Delete the problem
        mockMvc.perform(delete("/api/v1/problems/" + problem.getId()))
            .andExpect(status().isNoContent());

        // Then - Hypothesis and experiment should also be deleted (cascade)
        mockMvc.perform(get("/api/v1/hypotheses/" + hypothesis.getId()))
            .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/v1/experiments/" + experiment.getId()))
            .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = TEST_USER_ID)
    void shouldUpdateProblemAndMaintainRelationships() throws Exception {
        // Given - Create problem with hypothesis
        CreateProblemRequest problemRequest = new CreateProblemRequest("Original Title", "Original Description");
        MvcResult problemResult = mockMvc.perform(post("/api/v1/problems")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(problemRequest)))
            .andExpect(status().isCreated())
            .andReturn();
        ProblemDto problem = objectMapper.readValue(problemResult.getResponse().getContentAsString(), ProblemDto.class);

        CreateHypothesisRequest hypothesisRequest = new CreateHypothesisRequest(
            problem.getId(), "Test Hypothesis", "Test Outcome"
        );
        MvcResult hypothesisResult = mockMvc.perform(post("/api/v1/hypotheses")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(hypothesisRequest)))
            .andExpect(status().isCreated())
            .andReturn();
        HypothesisDto hypothesis = objectMapper.readValue(hypothesisResult.getResponse().getContentAsString(), HypothesisDto.class);

        // When - Update the problem
        UpdateProblemRequest updateRequest = new UpdateProblemRequest("Updated Title", "Updated Description");
        mockMvc.perform(put("/api/v1/problems/" + problem.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.title").value("Updated Title"));

        // Then - Hypothesis should still reference the problem
        mockMvc.perform(get("/api/v1/hypotheses/" + hypothesis.getId()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.problemId").value(problem.getId()));
    }

    @Test
    @WithMockUser(username = TEST_USER_ID)
    void shouldFilterExperimentsByMultipleCriteria() throws Exception {
        // Given - Create multiple experiments
        CreateProblemRequest problemRequest = new CreateProblemRequest("Test Problem", "Description");
        MvcResult problemResult = mockMvc.perform(post("/api/v1/problems")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(problemRequest)))
            .andExpect(status().isCreated())
            .andReturn();
        ProblemDto problem = objectMapper.readValue(problemResult.getResponse().getContentAsString(), ProblemDto.class);

        CreateHypothesisRequest hypothesis1Request = new CreateHypothesisRequest(
            problem.getId(), "Hypothesis 1", "Outcome 1"
        );
        MvcResult hypothesis1Result = mockMvc.perform(post("/api/v1/hypotheses")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(hypothesis1Request)))
            .andExpect(status().isCreated())
            .andReturn();
        HypothesisDto hypothesis1 = objectMapper.readValue(hypothesis1Result.getResponse().getContentAsString(), HypothesisDto.class);

        CreateHypothesisRequest hypothesis2Request = new CreateHypothesisRequest(
            problem.getId(), "Hypothesis 2", "Outcome 2"
        );
        MvcResult hypothesis2Result = mockMvc.perform(post("/api/v1/hypotheses")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(hypothesis2Request)))
            .andExpect(status().isCreated())
            .andReturn();
        HypothesisDto hypothesis2 = objectMapper.readValue(hypothesis2Result.getResponse().getContentAsString(), HypothesisDto.class);

        // Create experiments for hypothesis 1
        CreateExperimentRequest exp1Request = new CreateExperimentRequest(hypothesis1.getId(), "Experiment 1", "Desc");
        mockMvc.perform(post("/api/v1/experiments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(exp1Request)))
            .andExpect(status().isCreated());

        CreateExperimentRequest exp2Request = new CreateExperimentRequest(hypothesis1.getId(), "Experiment 2", "Desc");
        MvcResult exp2Result = mockMvc.perform(post("/api/v1/experiments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(exp2Request)))
            .andExpect(status().isCreated())
            .andReturn();
        ExperimentDto exp2 = objectMapper.readValue(exp2Result.getResponse().getContentAsString(), ExperimentDto.class);

        // Start one experiment
        mockMvc.perform(post("/api/v1/experiments/" + exp2.getId() + "/start"))
            .andExpect(status().isOk());

        // Create experiment for hypothesis 2
        CreateExperimentRequest exp3Request = new CreateExperimentRequest(hypothesis2.getId(), "Experiment 3", "Desc");
        mockMvc.perform(post("/api/v1/experiments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(exp3Request)))
            .andExpect(status().isCreated());

        // When & Then - Filter by hypothesis
        mockMvc.perform(get("/api/v1/experiments")
                .param("hypothesisId", hypothesis1.getId()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2));

        // Filter by status
        mockMvc.perform(get("/api/v1/experiments")
                .param("status", "DRAFT"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2));

        mockMvc.perform(get("/api/v1/experiments")
                .param("status", "RUNNING"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1));
    }
}
