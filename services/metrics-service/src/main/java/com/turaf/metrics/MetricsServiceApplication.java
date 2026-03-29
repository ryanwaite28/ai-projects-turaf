package com.turaf.metrics;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main application class for the Metrics Service.
 * 
 * This service handles metrics collection, aggregation, and analysis for
 * experiments and system monitoring in the Turaf platform.
 */
@SpringBootApplication(scanBasePackages = "com.turaf")
public class MetricsServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(MetricsServiceApplication.class, args);
    }
}
