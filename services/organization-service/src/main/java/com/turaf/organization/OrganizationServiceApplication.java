package com.turaf.organization;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main application class for the Organization Service.
 * 
 * This service handles organization/tenant management, including creation,
 * configuration, and multi-tenant data isolation for the Turaf platform.
 */
@SpringBootApplication(scanBasePackages = "com.turaf")
public class OrganizationServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrganizationServiceApplication.class, args);
    }
}
