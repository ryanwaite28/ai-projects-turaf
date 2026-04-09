package com.turaf.bff.controllers;

import com.turaf.bff.dto.ReportDto;
import com.turaf.bff.security.UserContext;
import com.turaf.bff.security.UserContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Report Query Controller - Provides read-only access to reports.
 * 
 * Reports are generated asynchronously by the Reporting Service (Lambda function)
 * when experiments are completed, following event-driven architecture:
 * 
 * Event Flow:
 * 1. Experiment Service publishes ExperimentCompleted event
 * 2. EventBridge routes event to Reporting Lambda
 * 3. Reporting Lambda generates report and stores in S3
 * 4. Reporting Lambda publishes ReportGenerated event
 * 
 * This controller provides query access to reports stored in S3/DynamoDB.
 * Report creation is NOT exposed via REST API - it violates event ownership.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
public class ReportController {
    
    @GetMapping
    public ResponseEntity<List<ReportDto>> getReports(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String experimentId,
            @RequestParam(required = false) String organizationId) {
        UserContext userContext = UserContextHolder.getCurrentUser();
        log.info("Query reports: type={}, status={}, experimentId={}, organizationId={}", 
            type, status, experimentId, organizationId);
        
        // TODO: Implement S3/DynamoDB query for report metadata
        // For now, return empty list (stub for testing)
        List<ReportDto> reports = new ArrayList<>();
        return ResponseEntity.ok(reports);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<ReportDto> getReport(@PathVariable String id) {
        UserContext userContext = UserContextHolder.getCurrentUser();
        log.info("Query report metadata: {}", id);
        
        // TODO: Implement S3/DynamoDB query for report metadata
        // For now, return stub data for testing
        ReportDto report = ReportDto.builder()
            .id(id)
            .type("EXPERIMENT")
            .format("PDF")
            .status("COMPLETED")
            .downloadUrl("https://example.com/reports/" + id + ".pdf")
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();
        
        return ResponseEntity.ok(report);
    }
    
    @GetMapping("/{id}/download")
    public ResponseEntity<byte[]> downloadReport(@PathVariable String id) {
        UserContext userContext = UserContextHolder.getCurrentUser();
        log.info("Download report from S3: {}", id);
        
        // TODO: Implement S3 download with presigned URL or direct streaming
        // For now, return stub PDF content for testing
        byte[] pdfContent = "%PDF-1.4\n1 0 obj\n<<\n/Type /Catalog\n/Pages 2 0 R\n>>\nendobj\n".getBytes();
        
        return ResponseEntity.ok()
            .header("Content-Type", "application/pdf")
            .header("Content-Disposition", "attachment; filename=report-" + id + ".pdf")
            .body(pdfContent);
    }
}
