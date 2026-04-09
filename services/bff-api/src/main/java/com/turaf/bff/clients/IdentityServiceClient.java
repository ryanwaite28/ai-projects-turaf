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
import org.springframework.web.client.RestClient;

@Slf4j
@Component
public class IdentityServiceClient {
    
    private final RestClient restClient;
    private static final String SERVICE_PATH = "/api/v1/auth";
    
    public IdentityServiceClient(@Qualifier("identityRestClient") RestClient restClient) {
        this.restClient = restClient;
    }
    
    public LoginResponseDto login(LoginRequest request) {
        log.debug("Calling Identity Service: POST /auth/login");
        LoginResponseDto response = restClient.post()
            .uri(SERVICE_PATH + "/login")
            .body(request)
            .retrieve()
            .body(LoginResponseDto.class);
        log.debug("Login successful for user: {}", response.getUser().getEmail());
        return response;
    }
    
    public LoginResponseDto register(RegisterRequest request) {
        log.debug("Calling Identity Service: POST /auth/register");
        LoginResponseDto response = restClient.post()
            .uri(SERVICE_PATH + "/register")
            .body(request)
            .retrieve()
            .body(LoginResponseDto.class);
        log.debug("Registration successful for user: {}", response.getUser().getEmail());
        return response;
    }
    
    public UserDto getCurrentUser(String userId) {
        log.debug("Calling Identity Service: GET /users/me");
        UserDto user = restClient.get()
            .uri("/api/v1/users/me")
            .header("X-User-Id", userId)
            .retrieve()
            .body(UserDto.class);
        log.debug("Retrieved current user: {}", user.getId());
        return user;
    }
    
    public void logout(String userId) {
        log.debug("Calling Identity Service: POST /auth/logout");
        restClient.post()
            .uri(SERVICE_PATH + "/logout")
            .header("X-User-Id", userId)
            .retrieve()
            .toBodilessEntity();
        log.debug("Logout successful");
    }
    
    public LoginResponseDto refreshToken(RefreshTokenRequest request) {
        log.debug("Calling Identity Service: POST /auth/refresh");
        LoginResponseDto response = restClient.post()
            .uri(SERVICE_PATH + "/refresh")
            .body(request)
            .retrieve()
            .body(LoginResponseDto.class);
        log.debug("Token refresh successful");
        return response;
    }
    
    public void requestPasswordReset(PasswordResetRequest request) {
        log.debug("Calling Identity Service: POST /auth/password-reset/request");
        restClient.post()
            .uri(SERVICE_PATH + "/password-reset/request")
            .body(request)
            .retrieve()
            .toBodilessEntity();
        log.debug("Password reset request successful");
    }
    
    public void confirmPasswordReset(PasswordResetConfirmRequest request) {
        log.debug("Calling Identity Service: POST /auth/password-reset/confirm");
        restClient.post()
            .uri(SERVICE_PATH + "/password-reset/confirm")
            .body(request)
            .retrieve()
            .toBodilessEntity();
        log.debug("Password reset confirmation successful");
    }
}
