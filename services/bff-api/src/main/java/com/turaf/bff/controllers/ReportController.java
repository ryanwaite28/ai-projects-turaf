package com.turaf.bff.controllers;

import com.turaf.bff.clients.ReportServiceClient;
import com.turaf.bff.dto.CreateReportRequest;
import com.turaf.bff.dto.ReportDto;
import com.turaf.bff.security.UserContext;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
public class ReportController {
    
    private final ReportServiceClient reportServiceClient;
    
    @GetMapping
    public Flux<ReportDto> getReports(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String status,
            @AuthenticationPrincipal UserContext userContext) {
        log.info("Get reports for organization: {}", userContext.getOrganizationId());
        return reportServiceClient.getReports(
                userContext.getUserId(), 
                userContext.getOrganizationId(),
                type,
                status)
            .doOnComplete(() -> log.info("Retrieved reports"))
            .doOnError(error -> log.error("Failed to get reports", error));
    }
    
    @PostMapping
    public Mono<ResponseEntity<ReportDto>> createReport(
            @Valid @RequestBody CreateReportRequest request,
            @AuthenticationPrincipal UserContext userContext) {
        log.info("Create report of type: {}", request.getType());
        return reportServiceClient.createReport(request, userContext.getUserId(), userContext.getOrganizationId())
            .map(ResponseEntity::ok)
            .doOnSuccess(response -> log.info("Report created"))
            .doOnError(error -> log.error("Failed to create report", error));
    }
    
    @GetMapping("/{id}")
    public Mono<ResponseEntity<ReportDto>> getReport(
            @PathVariable String id,
            @AuthenticationPrincipal UserContext userContext) {
        log.info("Get report: {}", id);
        return reportServiceClient.getReport(id, userContext.getUserId(), userContext.getOrganizationId())
            .map(ResponseEntity::ok)
            .doOnError(error -> log.error("Failed to get report {}", id, error));
    }
    
    @DeleteMapping("/{id}")
    public Mono<ResponseEntity<Void>> deleteReport(
            @PathVariable String id,
            @AuthenticationPrincipal UserContext userContext) {
        log.info("Delete report: {}", id);
        return reportServiceClient.deleteReport(id, userContext.getUserId(), userContext.getOrganizationId())
            .map(ResponseEntity::ok)
            .doOnSuccess(response -> log.info("Report deleted"))
            .doOnError(error -> log.error("Failed to delete report {}", id, error));
    }
    
    @GetMapping("/{id}/download")
    public Mono<ResponseEntity<byte[]>> downloadReport(
            @PathVariable String id,
            @AuthenticationPrincipal UserContext userContext) {
        log.info("Download report: {}", id);
        return reportServiceClient.downloadReport(id, userContext.getUserId(), userContext.getOrganizationId())
            .map(bytes -> ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=report-" + id + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(bytes))
            .doOnSuccess(response -> log.info("Report downloaded"))
            .doOnError(error -> log.error("Failed to download report {}", id, error));
    }
}
