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
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.List;

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
    public Mono<ResponseEntity<DashboardOverviewDto>> getDashboardOverview(
            @AuthenticationPrincipal UserContext userContext,
            @RequestHeader("Authorization") String authHeader) {
        
        log.info("Get dashboard overview for user: {}", userContext.getUserId());
        String token = authHeader.substring(7);
        
        Mono<UserDto> userMono = identityServiceClient.getCurrentUser(token)
            .doOnError(error -> log.error("Failed to get user", error))
            .onErrorReturn(UserDto.builder().build());
        
        Mono<List<OrganizationDto>> organizationsMono = 
            organizationServiceClient.getOrganizations(userContext.getUserId())
                .collectList()
                .doOnError(error -> log.error("Failed to get organizations", error))
                .onErrorReturn(Collections.emptyList());
        
        Mono<List<ExperimentDto>> experimentsMono = 
            userContext.getOrganizationId() != null
                ? experimentServiceClient.getExperiments(
                        userContext.getOrganizationId(), 
                        userContext.getUserId())
                    .filter(exp -> "RUNNING".equals(exp.getStatus()))
                    .collectList()
                    .doOnError(error -> log.error("Failed to get experiments", error))
                    .onErrorReturn(Collections.emptyList())
                : Mono.just(Collections.emptyList());
        
        return Mono.zip(userMono, organizationsMono, experimentsMono)
            .map(tuple -> {
                List<OrganizationDto> orgs = tuple.getT2();
                List<ExperimentDto> exps = tuple.getT3();
                
                DashboardOverviewDto overview = DashboardOverviewDto.builder()
                    .user(tuple.getT1())
                    .organizations(orgs)
                    .activeExperiments(exps)
                    .totalOrganizations(orgs.size())
                    .totalActiveExperiments(exps.size())
                    .build();
                return ResponseEntity.ok(overview);
            })
            .doOnSuccess(response -> log.info("Dashboard overview retrieved"))
            .doOnError(error -> log.error("Failed to get dashboard overview", error));
    }
    
    @GetMapping("/experiments/{id}/full")
    public Mono<ResponseEntity<ExperimentFullDto>> getExperimentFull(
            @PathVariable String id,
            @RequestParam String organizationId,
            @AuthenticationPrincipal UserContext userContext) {
        
        log.info("Get full experiment details: {}", id);
        
        Mono<ExperimentDto> experimentMono = experimentServiceClient.getExperiment(
                id, 
                userContext.getUserId(), 
                organizationId)
            .doOnError(error -> log.error("Failed to get experiment", error));
        
        Mono<List<MetricDto>> metricsMono = 
            metricsServiceClient.getExperimentMetrics(
                    id, 
                    userContext.getUserId(), 
                    organizationId)
                .collectList()
                .doOnError(error -> log.error("Failed to get metrics", error))
                .onErrorReturn(Collections.emptyList());
        
        return Mono.zip(experimentMono, metricsMono)
            .map(tuple -> {
                List<MetricDto> metrics = tuple.getT2();
                
                ExperimentFullDto fullDto = ExperimentFullDto.builder()
                    .experiment(tuple.getT1())
                    .metrics(metrics)
                    .totalMetrics(metrics.size())
                    .build();
                return ResponseEntity.ok(fullDto);
            })
            .doOnSuccess(response -> log.info("Full experiment details retrieved"))
            .doOnError(error -> log.error("Failed to get full experiment details", error));
    }
    
    @GetMapping("/organizations/{id}/summary")
    public Mono<ResponseEntity<OrganizationSummaryDto>> getOrganizationSummary(
            @PathVariable String id,
            @AuthenticationPrincipal UserContext userContext) {
        
        log.info("Get organization summary: {}", id);
        
        Mono<OrganizationDto> organizationMono = organizationServiceClient.getOrganization(
                id, 
                userContext.getUserId())
            .doOnError(error -> log.error("Failed to get organization", error));
        
        Mono<List<MemberDto>> membersMono = 
            organizationServiceClient.getMembers(id, userContext.getUserId())
                .collectList()
                .doOnError(error -> log.error("Failed to get members", error))
                .onErrorReturn(Collections.emptyList());
        
        Mono<List<ExperimentDto>> experimentsMono = 
            experimentServiceClient.getExperiments(id, userContext.getUserId())
                .collectList()
                .doOnError(error -> log.error("Failed to get experiments", error))
                .onErrorReturn(Collections.emptyList());
        
        return Mono.zip(organizationMono, membersMono, experimentsMono)
            .map(tuple -> {
                List<MemberDto> members = tuple.getT2();
                List<ExperimentDto> experiments = tuple.getT3();
                
                OrganizationSummaryDto summary = OrganizationSummaryDto.builder()
                    .organization(tuple.getT1())
                    .members(members)
                    .experiments(experiments)
                    .totalMembers(members.size())
                    .totalExperiments(experiments.size())
                    .build();
                return ResponseEntity.ok(summary);
            })
            .doOnSuccess(response -> log.info("Organization summary retrieved"))
            .doOnError(error -> log.error("Failed to get organization summary", error));
    }
}
