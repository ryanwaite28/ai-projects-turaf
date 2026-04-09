package com.turaf.bff.controllers;

import com.turaf.bff.clients.ExperimentServiceClient;
import com.turaf.bff.clients.IdentityServiceClient;
import com.turaf.bff.clients.MetricsServiceClient;
import com.turaf.bff.clients.OrganizationServiceClient;
import com.turaf.bff.dto.*;
import com.turaf.bff.security.UserContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
public class DashboardController {
    
    private final IdentityServiceClient identityServiceClient;
    private final OrganizationServiceClient organizationServiceClient;
    private final ExperimentServiceClient experimentServiceClient;
    private final MetricsServiceClient metricsServiceClient;
    
    @GetMapping("/overview")
    public ResponseEntity<DashboardOverviewDto> getDashboardOverview(
            @AuthenticationPrincipal UserContext userContext,
            @RequestHeader("Authorization") String authHeader) {
        
        log.info("Get dashboard overview for user: {}", userContext.getUserId());
        String token = authHeader.substring(7);
        
        UserDto user;
        try {
            user = identityServiceClient.getCurrentUser(token);
        } catch (Exception e) {
            log.error("Failed to get user", e);
            user = UserDto.builder().build();
        }
        
        List<OrganizationDto> organizations;
        try {
            organizations = organizationServiceClient.getOrganizations(userContext.getUserId());
        } catch (Exception e) {
            log.error("Failed to get organizations", e);
            organizations = Collections.emptyList();
        }
        
        List<ExperimentDto> experiments;
        if (userContext.getOrganizationId() != null) {
            try {
                List<ExperimentDto> allExperiments = experimentServiceClient.getExperiments(
                        userContext.getOrganizationId(), 
                        userContext.getUserId());
                experiments = allExperiments.stream()
                    .filter(exp -> "RUNNING".equals(exp.getStatus()))
                    .collect(Collectors.toList());
            } catch (Exception e) {
                log.error("Failed to get experiments", e);
                experiments = Collections.emptyList();
            }
        } else {
            experiments = Collections.emptyList();
        }
        
        DashboardOverviewDto overview = DashboardOverviewDto.builder()
            .user(user)
            .organizations(organizations)
            .activeExperiments(experiments)
            .totalOrganizations(organizations.size())
            .totalActiveExperiments(experiments.size())
            .build();
        
        log.info("Dashboard overview retrieved");
        return ResponseEntity.ok(overview);
    }
    
    @GetMapping("/experiments/{id}/full")
    public ResponseEntity<ExperimentFullDto> getExperimentFull(
            @PathVariable String id,
            @RequestParam String organizationId,
            @AuthenticationPrincipal UserContext userContext) {
        
        log.info("Get full experiment details: {}", id);
        
        ExperimentDto experiment = experimentServiceClient.getExperiment(
                id, 
                userContext.getUserId(), 
                organizationId);
        
        List<MetricDto> metrics;
        try {
            metrics = metricsServiceClient.getExperimentMetrics(
                    id, 
                    userContext.getUserId(), 
                    organizationId);
        } catch (Exception e) {
            log.error("Failed to get metrics", e);
            metrics = Collections.emptyList();
        }
        
        ExperimentFullDto fullDto = ExperimentFullDto.builder()
            .experiment(experiment)
            .metrics(metrics)
            .totalMetrics(metrics.size())
            .build();
        
        log.info("Full experiment details retrieved");
        return ResponseEntity.ok(fullDto);
    }
    
    @GetMapping("/organizations/{id}/summary")
    public ResponseEntity<OrganizationSummaryDto> getOrganizationSummary(
            @PathVariable String id,
            @AuthenticationPrincipal UserContext userContext) {
        
        log.info("Get organization summary: {}", id);
        
        OrganizationDto organization = organizationServiceClient.getOrganization(
                id, 
                userContext.getUserId());
        
        List<MemberDto> members;
        try {
            members = organizationServiceClient.getMembers(id, userContext.getUserId());
        } catch (Exception e) {
            log.error("Failed to get members", e);
            members = Collections.emptyList();
        }
        
        List<ExperimentDto> experiments;
        try {
            experiments = experimentServiceClient.getExperiments(id, userContext.getUserId());
        } catch (Exception e) {
            log.error("Failed to get experiments", e);
            experiments = Collections.emptyList();
        }
        
        OrganizationSummaryDto summary = OrganizationSummaryDto.builder()
            .organization(organization)
            .members(members)
            .experiments(experiments)
            .totalMembers(members.size())
            .totalExperiments(experiments.size())
            .build();
        
        log.info("Organization summary retrieved");
        return ResponseEntity.ok(summary);
    }
}
