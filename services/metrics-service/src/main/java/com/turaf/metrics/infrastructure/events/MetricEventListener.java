package com.turaf.metrics.infrastructure.events;

import com.turaf.metrics.domain.event.MetricBatchRecorded;
import com.turaf.metrics.domain.event.MetricRecorded;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
public class MetricEventListener {

    private static final Logger logger = LoggerFactory.getLogger(MetricEventListener.class);

    @EventListener
    @Async
    public void handleMetricRecorded(MetricRecorded event) {
        logger.info("Metric recorded: experimentId={}, metricName={}, value={}, type={}",
            event.getExperimentId(),
            event.getMetricName(),
            event.getValue(),
            event.getMetricType());
    }

    @EventListener
    @Async
    public void handleMetricBatchRecorded(MetricBatchRecorded event) {
        logger.info("Metric batch recorded: experimentId={}, count={}",
            event.getExperimentId(),
            event.getMetricCount());
    }
}
