package com.turaf.experiment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main application class for the Experiment Service.
 * 
 * This service handles A/B testing and experimentation, including experiment
 * creation, variant management, and result tracking for the Turaf platform.
 */
@SpringBootApplication(scanBasePackages = "com.turaf")
public class ExperimentServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ExperimentServiceApplication.class, args);
    }
}
