package com.turaf.identity;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main application class for the Identity Service.
 * 
 * This service handles user authentication, authorization, and identity management
 * for the Turaf platform.
 */
@SpringBootApplication(scanBasePackages = "com.turaf")
public class IdentityServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(IdentityServiceApplication.class, args);
    }
}
