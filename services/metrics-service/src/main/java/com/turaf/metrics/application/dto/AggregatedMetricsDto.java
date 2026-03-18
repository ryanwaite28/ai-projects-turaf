package com.turaf.metrics.application.dto;

import java.time.Instant;

public class AggregatedMetricsDto {

    private String metricName;
    private long count;
    private double sum;
    private double average;
    private double min;
    private double max;
    private Instant startTime;
    private Instant endTime;

    public AggregatedMetricsDto() {
    }

    public AggregatedMetricsDto(String metricName, long count, double sum, double average,
                               double min, double max, Instant startTime, Instant endTime) {
        this.metricName = metricName;
        this.count = count;
        this.sum = sum;
        this.average = average;
        this.min = min;
        this.max = max;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public static AggregatedMetricsDto empty() {
        return new AggregatedMetricsDto(
            null,
            0L,
            0.0,
            0.0,
            0.0,
            0.0,
            null,
            null
        );
    }

    public String getMetricName() {
        return metricName;
    }

    public void setMetricName(String metricName) {
        this.metricName = metricName;
    }

    public long getCount() {
        return count;
    }

    public void setCount(long count) {
        this.count = count;
    }

    public double getSum() {
        return sum;
    }

    public void setSum(double sum) {
        this.sum = sum;
    }

    public double getAverage() {
        return average;
    }

    public void setAverage(double average) {
        this.average = average;
    }

    public double getMin() {
        return min;
    }

    public void setMin(double min) {
        this.min = min;
    }

    public double getMax() {
        return max;
    }

    public void setMax(double max) {
        this.max = max;
    }

    public Instant getStartTime() {
        return startTime;
    }

    public void setStartTime(Instant startTime) {
        this.startTime = startTime;
    }

    public Instant getEndTime() {
        return endTime;
    }

    public void setEndTime(Instant endTime) {
        this.endTime = endTime;
    }
}
