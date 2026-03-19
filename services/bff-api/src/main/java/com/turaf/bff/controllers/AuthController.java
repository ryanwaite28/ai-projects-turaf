package com.turaf.bff.controllers;

import com.turaf.bff.clients.IdentityServiceClient;
import com.turaf.bff.dto.LoginRequest;
import com.turaf.bff.dto.RegisterRequest;
import com.turaf.bff.dto.UserDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {
    
    private final IdentityServiceClient identityServiceClient;
    
    @PostMapping("/login")
    public Mono<ResponseEntity<UserDto>> login(@Valid @RequestBody LoginRequest request) {
        log.info("Login request for email: {}", request.getEmail());
        return identityServiceClient.login(request)
            .map(ResponseEntity::ok)
            .doOnSuccess(response -> log.info("Login successful"))
            .doOnError(error -> log.error("Login failed", error));
    }
    
    @PostMapping("/register")
    public Mono<ResponseEntity<UserDto>> register(@Valid @RequestBody RegisterRequest request) {
        log.info("Register request for email: {}", request.getEmail());
        return identityServiceClient.register(request)
            .map(ResponseEntity::ok)
            .doOnSuccess(response -> log.info("Registration successful"))
            .doOnError(error -> log.error("Registration failed", error));
    }
    
    @GetMapping("/me")
    public Mono<ResponseEntity<UserDto>> getCurrentUser(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader.substring(7);
        log.info("Get current user request");
        return identityServiceClient.getCurrentUser(token)
            .map(ResponseEntity::ok)
            .doOnError(error -> log.error("Failed to get current user", error));
    }
    
    @PostMapping("/logout")
    public Mono<ResponseEntity<Void>> logout(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader.substring(7);
        log.info("Logout request");
        return identityServiceClient.logout(token)
            .map(ResponseEntity::ok)
            .doOnSuccess(response -> log.info("Logout successful"))
            .doOnError(error -> log.error("Logout failed", error));
    }
}
