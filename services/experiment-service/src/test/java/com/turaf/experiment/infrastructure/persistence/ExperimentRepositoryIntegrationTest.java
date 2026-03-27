package com.turaf.experiment.infrastructure.persistence;

import com.turaf.common.tenant.TenantContext;
import com.turaf.common.tenant.TenantContextHolder;
import com.turaf.experiment.domain.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

/**
 * Integration test for ExperimentRepository using Testcontainers.
 * Tests repository operations with a real PostgreSQL database.
 */
@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(ExperimentRepositoryImpl.class)
class ExperimentRepositoryIntegrationTest {
    
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
        .withDatabaseName("testdb")
        .withUsername("test")
        .withPassword("test");
    
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }
    
    @Autowired
    private ExperimentRepository experimentRepository;
    
    private String organizationId;
    private String userId;
    
    @BeforeEach
    void setUp() {
        organizationId = "org-test-123";
        userId = "user-test-456";
        TenantContextHolder.setContext(new TenantContext(organizationId, userId));
    }
    
    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }
    
    @Test
    void shouldSaveAndRetrieveExperiment() {
        // Given
        ExperimentId id = ExperimentId.generate();
        HypothesisId hypothesisId = HypothesisId.generate();
        Experiment experiment = new Experiment(
            id,
            organizationId,
            hypothesisId,
            "Test Experiment",
            "Test Description",
            userId
        );
        
        // When
        Experiment saved = experimentRepository.save(experiment);
        Optional<Experiment> retrieved = experimentRepository.findById(id);
        
        // Then
        assertThat(saved).isNotNull();
        assertThat(saved.getId()).isEqualTo(id);
        assertThat(saved.getOrganizationId()).isEqualTo(organizationId);
        
        assertThat(retrieved).isPresent();
        assertThat(retrieved.get().getName()).isEqualTo("Test Experiment");
        assertThat(retrieved.get().getOrganizationId()).isEqualTo(organizationId);
    }
    
    @Test
    void shouldFilterByOrganizationId() {
        // Given - Create experiments for two different organizations
        String org1 = "org-1";
        String org2 = "org-2";
        
        TenantContextHolder.setContext(new TenantContext(org1, userId));
        Experiment exp1 = createExperiment(org1, "Experiment 1");
        experimentRepository.save(exp1);
        
        TenantContextHolder.setContext(new TenantContext(org2, userId));
        Experiment exp2 = createExperiment(org2, "Experiment 2");
        experimentRepository.save(exp2);
        
        // When - Query for org1's experiments
        TenantContextHolder.setContext(new TenantContext(org1, userId));
        List<Experiment> org1Experiments = experimentRepository.findByOrganizationId(org1);
        
        // Then - Should only get org1's experiment
        assertThat(org1Experiments).hasSize(1);
        assertThat(org1Experiments.get(0).getName()).isEqualTo("Experiment 1");
        assertThat(org1Experiments.get(0).getOrganizationId()).isEqualTo(org1);
    }
    
    @Test
    void shouldFindByStatus() {
        // Given
        Experiment draftExperiment = createExperiment(organizationId, "Draft Experiment");
        experimentRepository.save(draftExperiment);
        
        Experiment runningExperiment = createExperiment(organizationId, "Running Experiment");
        runningExperiment.start();
        experimentRepository.save(runningExperiment);
        
        // When
        List<Experiment> draftExperiments = experimentRepository.findByStatus(ExperimentStatus.DRAFT);
        List<Experiment> runningExperiments = experimentRepository.findByStatus(ExperimentStatus.RUNNING);
        
        // Then
        assertThat(draftExperiments).hasSize(1);
        assertThat(draftExperiments.get(0).getStatus()).isEqualTo(ExperimentStatus.DRAFT);
        
        assertThat(runningExperiments).hasSize(1);
        assertThat(runningExperiments.get(0).getStatus()).isEqualTo(ExperimentStatus.RUNNING);
    }
    
    @Test
    void shouldDeleteExperiment() {
        // Given
        ExperimentId id = ExperimentId.generate();
        Experiment experiment = createExperiment(organizationId, "To Delete");
        experiment = new Experiment(id, organizationId, HypothesisId.generate(), "To Delete", "Description", userId);
        experimentRepository.save(experiment);
        
        // When
        experimentRepository.delete(experiment);
        Optional<Experiment> retrieved = experimentRepository.findById(id);
        
        // Then
        assertThat(retrieved).isEmpty();
    }
    
    @Test
    void shouldCheckExistence() {
        // Given
        ExperimentId id = ExperimentId.generate();
        Experiment experiment = new Experiment(id, organizationId, HypothesisId.generate(), "Test", "Description", userId);
        experimentRepository.save(experiment);
        
        // When/Then
        assertThat(experimentRepository.existsById(id)).isTrue();
        assertThat(experimentRepository.existsById(ExperimentId.generate())).isFalse();
    }
    
    private Experiment createExperiment(String orgId, String name) {
        return new Experiment(
            ExperimentId.generate(),
            orgId,
            HypothesisId.generate(),
            name,
            "Test Description",
            userId
        );
    }
}
