package com.turaf.bff.clients;

import com.turaf.bff.dto.CreateReportRequest;
import com.turaf.bff.dto.ReportDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@Component
public class ReportServiceClient {
    
    private final WebClient webClient;
    private static final String SERVICE_PATH = "/reports";
    
    public ReportServiceClient(@Qualifier("reportWebClient") WebClient webClient) {
        this.webClient = webClient;
    }
    
    public Flux<ReportDto> getReports(String userId, String organizationId, String type, String status) {
        log.debug("Calling Report Service: GET /reports");
        
        return webClient.get()
            .uri(uriBuilder -> {
                var builder = uriBuilder.path(SERVICE_PATH + "/reports");
                if (type != null) builder.queryParam("type", type);
                if (status != null) builder.queryParam("status", status);
                return builder.build();
            })
            .header("X-User-Id", userId)
            .header("X-Organization-Id", organizationId)
            .retrieve()
            .bodyToFlux(ReportDto.class)
            .doOnComplete(() -> log.debug("Retrieved reports"))
            .doOnError(error -> log.error("Failed to get reports", error));
    }
    
    public Mono<ReportDto> createReport(CreateReportRequest request, String userId, String organizationId) {
        log.debug("Calling Report Service: POST /reports");
        return webClient.post()
            .uri(SERVICE_PATH + "/reports")
            .header("X-User-Id", userId)
            .header("X-Organization-Id", organizationId)
            .bodyValue(request)
            .retrieve()
            .bodyToMono(ReportDto.class)
            .doOnSuccess(report -> log.debug("Report created: {}", report.getId()))
            .doOnError(error -> log.error("Failed to create report", error));
    }
    
    public Mono<ReportDto> getReport(String id, String userId, String organizationId) {
        log.debug("Calling Report Service: GET /reports/{}", id);
        return webClient.get()
            .uri(SERVICE_PATH + "/reports/{id}", id)
            .header("X-User-Id", userId)
            .header("X-Organization-Id", organizationId)
            .retrieve()
            .bodyToMono(ReportDto.class)
            .doOnSuccess(report -> log.debug("Retrieved report: {}", id))
            .doOnError(error -> log.error("Failed to get report: {}", id, error));
    }
    
    public Mono<Void> deleteReport(String id, String userId, String organizationId) {
        log.debug("Calling Report Service: DELETE /reports/{}", id);
        return webClient.delete()
            .uri(SERVICE_PATH + "/reports/{id}", id)
            .header("X-User-Id", userId)
            .header("X-Organization-Id", organizationId)
            .retrieve()
            .bodyToMono(Void.class)
            .doOnSuccess(v -> log.debug("Report deleted: {}", id))
            .doOnError(error -> log.error("Failed to delete report: {}", id, error));
    }
    
    public Mono<byte[]> downloadReport(String id, String userId, String organizationId) {
        log.debug("Calling Report Service: GET /reports/{}/download", id);
        return webClient.get()
            .uri(SERVICE_PATH + "/reports/{id}/download", id)
            .header("X-User-Id", userId)
            .header("X-Organization-Id", organizationId)
            .retrieve()
            .bodyToMono(byte[].class)
            .doOnSuccess(bytes -> log.debug("Downloaded report: {}", id))
            .doOnError(error -> log.error("Failed to download report: {}", id, error));
    }
}
