package com.turaf.bff.clients;

import com.turaf.bff.dto.LoginRequest;
import com.turaf.bff.dto.LoginResponseDto;
import com.turaf.bff.dto.PasswordResetConfirmRequest;
import com.turaf.bff.dto.PasswordResetRequest;
import com.turaf.bff.dto.RefreshTokenRequest;
import com.turaf.bff.dto.RegisterRequest;
import com.turaf.bff.dto.UserDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Slf4j
@Component
public class IdentityServiceClient {
    
    private final WebClient webClient;
    private static final String SERVICE_PATH = "/api/v1";
    
    public IdentityServiceClient(@Qualifier("identityWebClient") WebClient webClient) {
        this.webClient = webClient;
    }
    
    public Mono<LoginResponseDto> login(LoginRequest request) {
        log.debug("Calling Identity Service: POST /auth/login");
        return webClient.post()
            .uri(SERVICE_PATH + "/auth/login")
            .bodyValue(request)
            .retrieve()
            .bodyToMono(LoginResponseDto.class)
            .doOnSuccess(response -> log.debug("Login successful for user: {}", response.getUser().getEmail()))
            .doOnError(error -> log.error("Failed to login", error));
    }
    
    public Mono<LoginResponseDto> register(RegisterRequest request) {
        log.debug("Calling Identity Service: POST /auth/register");
        return webClient.post()
            .uri(SERVICE_PATH + "/auth/register")
            .bodyValue(request)
            .retrieve()
            .bodyToMono(LoginResponseDto.class)
            .doOnSuccess(response -> log.debug("Registration successful for user: {}", response.getUser().getEmail()))
            .doOnError(error -> log.error("Failed to register", error));
    }
    
    public Mono<UserDto> getCurrentUser(String userId) {
        log.debug("Calling Identity Service: GET /users/me");
        return webClient.get()
            .uri(SERVICE_PATH + "/users/me")
            .header("X-User-Id", userId)
            .retrieve()
            .bodyToMono(UserDto.class)
            .doOnSuccess(user -> log.debug("Retrieved current user: {}", user.getId()))
            .doOnError(error -> log.error("Failed to get current user", error));
    }
    
    public Mono<Void> logout(String userId) {
        log.debug("Calling Identity Service: POST /auth/logout");
        return webClient.post()
            .uri(SERVICE_PATH + "/auth/logout")
            .header("X-User-Id", userId)
            .retrieve()
            .bodyToMono(Void.class)
            .doOnSuccess(v -> log.debug("Logout successful"))
            .doOnError(error -> log.error("Failed to logout", error));
    }
    
    public Mono<LoginResponseDto> refreshToken(RefreshTokenRequest request) {
        log.debug("Calling Identity Service: POST /auth/refresh");
        return webClient.post()
            .uri(SERVICE_PATH + "/auth/refresh")
            .bodyValue(request)
            .retrieve()
            .bodyToMono(LoginResponseDto.class)
            .doOnSuccess(response -> log.debug("Token refresh successful"))
            .doOnError(error -> log.error("Failed to refresh token", error));
    }
    
    public Mono<Void> requestPasswordReset(PasswordResetRequest request) {
        log.debug("Calling Identity Service: POST /auth/password-reset/request");
        return webClient.post()
            .uri(SERVICE_PATH + "/auth/password-reset/request")
            .bodyValue(request)
            .retrieve()
            .bodyToMono(Void.class)
            .doOnSuccess(v -> log.debug("Password reset request successful"))
            .doOnError(error -> log.error("Failed to request password reset", error));
    }
    
    public Mono<Void> confirmPasswordReset(PasswordResetConfirmRequest request) {
        log.debug("Calling Identity Service: POST /auth/password-reset/confirm");
        return webClient.post()
            .uri(SERVICE_PATH + "/auth/password-reset/confirm")
            .bodyValue(request)
            .retrieve()
            .bodyToMono(Void.class)
            .doOnSuccess(v -> log.debug("Password reset confirmation successful"))
            .doOnError(error -> log.error("Failed to confirm password reset", error));
    }
}
