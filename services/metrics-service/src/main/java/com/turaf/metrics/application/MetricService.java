package com.turaf.metrics.application;

import com.turaf.common.event.EventPublisher;
import com.turaf.common.multitenancy.TenantContextHolder;
import com.turaf.metrics.application.dto.MetricDto;
import com.turaf.metrics.application.dto.RecordMetricRequest;
import com.turaf.metrics.domain.*;
import com.turaf.metrics.domain.event.MetricBatchRecorded;
import com.turaf.metrics.domain.event.MetricRecorded;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class MetricService {

    private final MetricRepository metricRepository;
    private final EventPublisher eventPublisher;

    public MetricService(MetricRepository metricRepository, EventPublisher eventPublisher) {
        this.metricRepository = metricRepository;
        this.eventPublisher = eventPublisher;
    }

    public MetricDto recordMetric(RecordMetricRequest request) {
        String organizationId = TenantContextHolder.getOrganizationId();
        MetricId id = MetricId.generate();

        Metric metric = new Metric(
            id,
            organizationId,
            request.getExperimentId(),
            request.getName(),
            request.getValue(),
            MetricType.valueOf(request.getType()),
            request.getTimestamp() != null ? request.getTimestamp() : Instant.now()
        );

        if (request.getTags() != null) {
            request.getTags().forEach(metric::addTag);
        }

        metricRepository.save(metric);

        MetricRecorded event = new MetricRecorded(
            UUID.randomUUID().toString(),
            organizationId,
            request.getExperimentId(),
            id.getValue(),
            request.getName(),
            request.getValue(),
            request.getType(),
            metric.getTimestamp()
        );
        eventPublisher.publish(event);

        return MetricDto.fromDomain(metric);
    }

    public List<MetricDto> recordBatch(List<RecordMetricRequest> requests) {
        String organizationId = TenantContextHolder.getOrganizationId();

        List<Metric> metrics = requests.stream()
            .map(request -> {
                MetricId id = MetricId.generate();
                Metric metric = new Metric(
                    id,
                    organizationId,
                    request.getExperimentId(),
                    request.getName(),
                    request.getValue(),
                    MetricType.valueOf(request.getType()),
                    request.getTimestamp() != null ? request.getTimestamp() : Instant.now()
                );

                if (request.getTags() != null) {
                    request.getTags().forEach(metric::addTag);
                }

                return metric;
            })
            .collect(Collectors.toList());

        metricRepository.saveAll(metrics);

        if (!requests.isEmpty()) {
            String experimentId = requests.get(0).getExperimentId();
            MetricBatchRecorded event = new MetricBatchRecorded(
                UUID.randomUUID().toString(),
                organizationId,
                experimentId,
                metrics.size()
            );
            eventPublisher.publish(event);
        }

        return metrics.stream()
            .map(MetricDto::fromDomain)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<MetricDto> getMetrics(String experimentId, String name, Instant start, Instant end) {
        List<Metric> metrics;
        if (name != null && !name.isBlank()) {
            metrics = metricRepository.findByExperimentIdAndName(experimentId, name, start, end);
        } else {
            metrics = metricRepository.findByExperimentId(experimentId, start, end);
        }
        return metrics.stream()
            .map(MetricDto::fromDomain)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<MetricDto> getMetricsByOrganization(String organizationId, Instant start, Instant end) {
        List<Metric> metrics = metricRepository.findByOrganizationId(organizationId, start, end);
        return metrics.stream()
            .map(MetricDto::fromDomain)
            .collect(Collectors.toList());
    }

    public void deleteMetricsByExperiment(String experimentId) {
        metricRepository.deleteByExperimentId(experimentId);
    }
}
