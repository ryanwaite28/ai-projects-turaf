package com.turaf.bff.controllers;

import com.turaf.bff.clients.IdentityServiceClient;
import com.turaf.bff.dto.LoginRequest;
import com.turaf.bff.dto.LoginResponseDto;
import com.turaf.bff.dto.PasswordResetConfirmRequest;
import com.turaf.bff.dto.PasswordResetRequest;
import com.turaf.bff.dto.RefreshTokenRequest;
import com.turaf.bff.dto.RegisterRequest;
import com.turaf.bff.dto.UserDto;
import com.turaf.bff.security.UserContext;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {
    
    private final IdentityServiceClient identityServiceClient;
    
    @PostMapping("/login")
    public Mono<ResponseEntity<LoginResponseDto>> login(@Valid @RequestBody LoginRequest request) {
        log.info("Login request for email: {}", request.getEmail());
        return identityServiceClient.login(request)
            .map(ResponseEntity::ok)
            .doOnSuccess(response -> log.info("Login successful"))
            .doOnError(error -> log.error("Login failed", error));
    }
    
    @PostMapping("/register")
    public Mono<ResponseEntity<LoginResponseDto>> register(@Valid @RequestBody RegisterRequest request) {
        log.info("Register request for email: {}", request.getEmail());
        return identityServiceClient.register(request)
            .map(ResponseEntity::ok)
            .doOnSuccess(response -> log.info("Registration successful"))
            .doOnError(error -> log.error("Registration failed", error));
    }
    
    @GetMapping("/me")
    public Mono<ResponseEntity<UserDto>> getCurrentUser(@AuthenticationPrincipal UserContext userContext) {
        log.info("Get current user request for userId: {}", userContext.getUserId());
        return identityServiceClient.getCurrentUser(userContext.getUserId())
            .map(ResponseEntity::ok)
            .doOnError(error -> log.error("Failed to get current user", error));
    }
    
    @PostMapping("/logout")
    public Mono<ResponseEntity<Void>> logout(@AuthenticationPrincipal UserContext userContext) {
        log.info("Logout request for userId: {}", userContext.getUserId());
        return identityServiceClient.logout(userContext.getUserId())
            .map(ResponseEntity::ok)
            .doOnSuccess(response -> log.info("Logout successful"))
            .doOnError(error -> log.error("Logout failed", error));
    }
    
    @PostMapping("/refresh")
    public Mono<ResponseEntity<LoginResponseDto>> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        log.info("Token refresh request");
        return identityServiceClient.refreshToken(request)
            .map(ResponseEntity::ok)
            .doOnSuccess(response -> log.info("Token refresh successful"))
            .doOnError(error -> log.error("Token refresh failed", error));
    }
    
    @PostMapping("/password-reset/request")
    public Mono<ResponseEntity<Void>> requestPasswordReset(@Valid @RequestBody PasswordResetRequest request) {
        log.info("Password reset request for email: {}", request.getEmail());
        return identityServiceClient.requestPasswordReset(request)
            .map(ResponseEntity::ok)
            .doOnSuccess(response -> log.info("Password reset request successful"))
            .doOnError(error -> log.error("Password reset request failed", error));
    }
    
    @PostMapping("/password-reset/confirm")
    public Mono<ResponseEntity<Void>> confirmPasswordReset(@Valid @RequestBody PasswordResetConfirmRequest request) {
        log.info("Password reset confirmation request");
        return identityServiceClient.confirmPasswordReset(request)
            .map(ResponseEntity::ok)
            .doOnSuccess(response -> log.info("Password reset confirmation successful"))
            .doOnError(error -> log.error("Password reset confirmation failed", error));
    }
}
