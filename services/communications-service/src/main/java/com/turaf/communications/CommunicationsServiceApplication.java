package com.turaf.communications;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EnableJpaRepositories
public class CommunicationsServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(CommunicationsServiceApplication.class, args);
    }
}
