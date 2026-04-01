package com.turaf.bff.clients;

import com.turaf.bff.dto.LoginRequest;
import com.turaf.bff.dto.LoginResponseDto;
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
    private static final String SERVICE_PATH = "/identity";
    
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
}
