package com.turaf.bff;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class BffApiApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(BffApiApplication.class, args);
    }
}
