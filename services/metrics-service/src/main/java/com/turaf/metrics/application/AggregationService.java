package com.turaf.metrics.application;

import com.turaf.metrics.application.dto.AggregatedMetricsDto;
import com.turaf.metrics.domain.Metric;
import com.turaf.metrics.domain.MetricRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.DoubleSummaryStatistics;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class AggregationService {

    private final MetricRepository metricRepository;

    public AggregationService(MetricRepository metricRepository) {
        this.metricRepository = metricRepository;
    }

    public AggregatedMetricsDto aggregateMetrics(String experimentId, String metricName,
                                                 Instant start, Instant end) {
        List<Metric> metrics = metricRepository.findByExperimentIdAndName(
            experimentId, metricName, start, end
        );

        if (metrics.isEmpty()) {
            return AggregatedMetricsDto.empty();
        }

        return calculateStats(metricName, metrics, start, end);
    }

    public Map<String, AggregatedMetricsDto> aggregateAllMetrics(String experimentId,
                                                                  Instant start, Instant end) {
        List<Metric> metrics = metricRepository.findByExperimentId(experimentId, start, end);

        if (metrics.isEmpty()) {
            return Map.of();
        }

        return metrics.stream()
            .collect(Collectors.groupingBy(Metric::getName))
            .entrySet()
            .stream()
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                entry -> calculateStats(entry.getKey(), entry.getValue(), start, end)
            ));
    }

    public Map<String, Double> getStatistics(String experimentId, String metricName) {
        return metricRepository.aggregateByExperiment(experimentId, metricName);
    }

    public AggregatedMetricsDto aggregateByOrganization(String organizationId, String metricName,
                                                        Instant start, Instant end) {
        List<Metric> metrics = metricRepository.findByOrganizationId(organizationId, start, end)
            .stream()
            .filter(m -> m.getName().equals(metricName))
            .collect(Collectors.toList());

        if (metrics.isEmpty()) {
            return AggregatedMetricsDto.empty();
        }

        return calculateStats(metricName, metrics, start, end);
    }

    private AggregatedMetricsDto calculateStats(String name, List<Metric> metrics,
                                                Instant start, Instant end) {
        DoubleSummaryStatistics stats = metrics.stream()
            .mapToDouble(Metric::getValue)
            .summaryStatistics();

        return new AggregatedMetricsDto(
            name,
            stats.getCount(),
            stats.getSum(),
            stats.getAverage(),
            stats.getMin(),
            stats.getMax(),
            start,
            end
        );
    }
}
