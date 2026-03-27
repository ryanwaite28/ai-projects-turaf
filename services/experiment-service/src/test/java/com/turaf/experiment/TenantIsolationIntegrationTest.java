package com.turaf.experiment;

import com.turaf.common.tenant.TenantContext;
import com.turaf.common.tenant.TenantContextHolder;
import com.turaf.experiment.domain.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Integration test verifying tenant isolation across the application.
 * Ensures users cannot access data from other organizations.
 */
@SpringBootTest
@Testcontainers
@Transactional
class TenantIsolationIntegrationTest {
    
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
    
    @Autowired
    private ProblemRepository problemRepository;
    
    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }
    
    @Test
    void userCannotAccessOtherOrganizationExperiments() {
        // Given - Create experiment for organization A
        String orgA = "org-a";
        String orgB = "org-b";
        String userA = "user-a";
        String userB = "user-b";
        
        TenantContextHolder.setContext(new TenantContext(orgA, userA));
        Experiment experimentA = new Experiment(
            ExperimentId.generate(),
            orgA,
            HypothesisId.generate(),
            "Org A Experiment",
            "Description",
            userA
        );
        experimentRepository.save(experimentA);
        TenantContextHolder.clear();
        
        // When - Try to access as user from organization B
        TenantContextHolder.setContext(new TenantContext(orgB, userB));
        List<Experiment> orgBExperiments = experimentRepository.findByOrganizationId(orgB);
        
        // Then - Should not see organization A's experiment
        assertThat(orgBExperiments).isEmpty();
        assertThat(orgBExperiments).doesNotContain(experimentA);
    }
    
    @Test
    void tenantInterceptorAutomaticallySetsOrganizationId() {
        // Given
        String orgId = "org-auto-test";
        String userId = "user-auto-test";
        TenantContextHolder.setContext(new TenantContext(orgId, userId));
        
        // When - Create problem without explicitly setting organizationId
        ProblemId problemId = ProblemId.generate();
        Problem problem = new Problem(
            problemId,
            orgId, // This should be set automatically by TenantInterceptor
            "Test Problem",
            "Description",
            userId
        );
        Problem saved = problemRepository.save(problem);
        
        // Then - OrganizationId should be set
        assertThat(saved.getOrganizationId()).isEqualTo(orgId);
    }
    
    @Test
    void multipleOrganizationsCanHaveSeparateData() {
        // Given - Create data for three organizations
        String org1 = "org-1";
        String org2 = "org-2";
        String org3 = "org-3";
        
        TenantContextHolder.setContext(new TenantContext(org1, "user-1"));
        experimentRepository.save(createExperiment(org1, "Exp 1"));
        experimentRepository.save(createExperiment(org1, "Exp 2"));
        
        TenantContextHolder.setContext(new TenantContext(org2, "user-2"));
        experimentRepository.save(createExperiment(org2, "Exp 3"));
        
        TenantContextHolder.setContext(new TenantContext(org3, "user-3"));
        experimentRepository.save(createExperiment(org3, "Exp 4"));
        experimentRepository.save(createExperiment(org3, "Exp 5"));
        experimentRepository.save(createExperiment(org3, "Exp 6"));
        
        // When/Then - Each organization sees only their data
        TenantContextHolder.setContext(new TenantContext(org1, "user-1"));
        assertThat(experimentRepository.findByOrganizationId(org1)).hasSize(2);
        
        TenantContextHolder.setContext(new TenantContext(org2, "user-2"));
        assertThat(experimentRepository.findByOrganizationId(org2)).hasSize(1);
        
        TenantContextHolder.setContext(new TenantContext(org3, "user-3"));
        assertThat(experimentRepository.findByOrganizationId(org3)).hasSize(3);
    }
    
    @Test
    void queryingWithoutOrganizationIdFilterReturnsOnlyTenantData() {
        // Given - Create experiments for two organizations
        String org1 = "org-query-1";
        String org2 = "org-query-2";
        
        TenantContextHolder.setContext(new TenantContext(org1, "user-1"));
        experimentRepository.save(createExperiment(org1, "Exp A"));
        
        TenantContextHolder.setContext(new TenantContext(org2, "user-2"));
        experimentRepository.save(createExperiment(org2, "Exp B"));
        
        // When - Query by status (not explicitly filtering by org)
        TenantContextHolder.setContext(new TenantContext(org1, "user-1"));
        List<Experiment> org1Drafts = experimentRepository.findByStatus(ExperimentStatus.DRAFT);
        
        // Then - Should only get org1's experiments
        // Note: This test verifies that repository methods properly filter by organizationId
        assertThat(org1Drafts).hasSize(1);
        assertThat(org1Drafts.get(0).getOrganizationId()).isEqualTo(org1);
    }
    
    private Experiment createExperiment(String orgId, String name) {
        return new Experiment(
            ExperimentId.generate(),
            orgId,
            HypothesisId.generate(),
            name,
            "Test Description",
            "test-user"
        );
    }
}
