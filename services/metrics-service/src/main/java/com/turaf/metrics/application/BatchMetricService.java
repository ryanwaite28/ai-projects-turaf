package com.turaf.metrics.application;

import com.turaf.common.event.EventPublisher;
import com.turaf.common.multitenancy.TenantContextHolder;
import com.turaf.metrics.application.dto.BatchRecordRequest;
import com.turaf.metrics.application.dto.RecordMetricRequest;
import com.turaf.metrics.domain.*;
import com.turaf.metrics.domain.event.MetricBatchRecorded;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class BatchMetricService {

    private final MetricRepository metricRepository;
    private final EventPublisher eventPublisher;

    private static final int BATCH_SIZE = 1000;

    public BatchMetricService(MetricRepository metricRepository, EventPublisher eventPublisher) {
        this.metricRepository = metricRepository;
        this.eventPublisher = eventPublisher;
    }

    public void recordBatch(BatchRecordRequest request) {
        String organizationId = TenantContextHolder.getOrganizationId();

        List<Metric> metrics = request.getMetrics().stream()
            .map(req -> createMetric(req, organizationId))
            .collect(Collectors.toList());

        // Process in batches for large datasets
        List<List<Metric>> batches = partition(metrics, BATCH_SIZE);
        for (List<Metric> batch : batches) {
            metricRepository.saveAll(batch);
        }

        // Publish batch event
        MetricBatchRecorded event = new MetricBatchRecorded(
            UUID.randomUUID().toString(),
            organizationId,
            request.getExperimentId(),
            metrics.size()
        );
        eventPublisher.publish(event);
    }

    public int recordBatchAndReturnCount(BatchRecordRequest request) {
        String organizationId = TenantContextHolder.getOrganizationId();

        List<Metric> metrics = request.getMetrics().stream()
            .map(req -> createMetric(req, organizationId))
            .collect(Collectors.toList());

        List<List<Metric>> batches = partition(metrics, BATCH_SIZE);
        for (List<Metric> batch : batches) {
            metricRepository.saveAll(batch);
        }

        MetricBatchRecorded event = new MetricBatchRecorded(
            UUID.randomUUID().toString(),
            organizationId,
            request.getExperimentId(),
            metrics.size()
        );
        eventPublisher.publish(event);

        return metrics.size();
    }

    private Metric createMetric(RecordMetricRequest req, String organizationId) {
        MetricId id = MetricId.generate();
        Metric metric = new Metric(
            id,
            organizationId,
            req.getExperimentId(),
            req.getName(),
            req.getValue(),
            MetricType.valueOf(req.getType()),
            req.getTimestamp() != null ? req.getTimestamp() : Instant.now()
        );

        if (req.getTags() != null) {
            req.getTags().forEach(metric::addTag);
        }

        return metric;
    }

    private <T> List<List<T>> partition(List<T> list, int size) {
        List<List<T>> partitions = new ArrayList<>();
        for (int i = 0; i < list.size(); i += size) {
            partitions.add(list.subList(i, Math.min(i + size, list.size())));
        }
        return partitions;
    }
}
